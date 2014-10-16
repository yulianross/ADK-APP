package com.example.adk_app;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

public class MyService extends Service implements Runnable{
	
	private static final String TAG = "HelloLED";
	 private static final String ACTION_USB_PERMISSION = "com.uphyca.android.app.helloled.action.USB_PERMISSION";

	    private PendingIntent mPermissionIntent;
	    private boolean mPermissionRequestPending;

	    private UsbManager mUsbManager;
	    private UsbAccessory mAccessory;

	    ParcelFileDescriptor mFileDescriptor;

	    FileInputStream mInputStream;
	    FileOutputStream mOutputStream;
	    
	  
	    
	    private static boolean isRunning = false;
	    
	    private static final byte COMMAND_SERVO = 0x7;
	    private static final byte SERVO_ID_1 = 0x1;
	    private static final byte SERVO_ID_2=0x2;
	    private static final byte COMMAND_CONTACT=0x3;
	    private SensorManager sensorManager;
	    private Sensor accelerometer;
	    
	    boolean servo=false;
	    boolean siguelineas=false;
	    byte contact;
	    boolean crash=false;
	    
	    //El broadcastReceiver se encarga de recibir el evento del sistema si se intenta conectar un dispositivo
	    //al USB del teléfono y en le caso de que sea el arduino establece conexión con él. Toda la información sobre
	    //este elelemento en android developers.
	    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver(){
	    	
	    	 @Override
	         public void onReceive(Context context, Intent intent){
	    		 String action = intent.getAction();

	             if (ACTION_USB_PERMISSION.equals(action)){
	            	 synchronized (this){
	            		 UsbAccessory accessory =  (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

	                    
	                     if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
	                    	 openAccessory(accessory);
	                     }else{
	                    	 Log.d(TAG, "permission denied for accessory " + accessory);
	                     }
	                     mPermissionRequestPending = false;
	            	 }
	             }else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)){
	            	 UsbAccessory accessory =  (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
	                 if (accessory != null && accessory.equals(mAccessory)){
	                	 closeAccessory();
	                 }
	             }
	    	 }
	    };
	    
	 
	    //función para convertir dos variables de tipo byte en una de tipo int. 
	    private int composeInt(byte hi, byte lo) {
			int val = (int) hi & 0xff;
			val *= 256;
			val += (int) lo & 0xff;
			return val;
		}
	    
	    
	
	
	   
	    //array para almacenar todas las actividades cliente que se unen a este servicio
	    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
	    int mValue = 0; // Holds last value set by a client.
	    //variables para identificar de que sensor es el mensaje que se manda a la actividad cliente
	    static final int MSG_REGISTER_CLIENT = 1;
	    static final int MSG_UNREGISTER_CLIENT = 2;
	    static final int MSG_SET_BYTE_VALUE = 3;
	    static final int MSG_SERVO_1=4;
	    static final int MSG_SERVO_2=5;
	    static final int MSG_LIGHT=6;
	    static final int MSG_ULTRASONIC=7;
	    static final int MSG_RIGHT_CONTACT=8;
	    static final int MSG_LEFT_CONTACT=9;
	    static final int MSG_REGISTER_CLIENT2=10;
	    static final int MSG_UNREGISTER_CLIENT2=11;
	    static final int MSG_CONTACT_VALUE=12;
	
	    
	    //6. Recibe el mensaje del client, y el mensaje lo manda al incomingHandler , para que haga 
	    // con el lo que le de la gana
	    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.


	    
	    // 4. Cuando presiono el botón "bind to the service, se manda una peticion a través de un intent a 
	    //esta función, onbind(). Esta devuelve una referencia a mMessenger y un dispath callback a la funcion
	    //onServiceConnected() en el ServiceConnection para establecer la comunicacion a traves de un Ibinder
	    // con el client que se conecte al Service.
	    @Override
	    public IBinder onBind(Intent intent) {
	        return mMessenger.getBinder();
	    }
	    //7.una vez que hemos recibido el mensaje del Client con esta clase decidimos qué hacemos con él.
	    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
	        @Override
	        public void handleMessage(Message msg) {
	            switch (msg.what) {
	            //El caso de que se una  ManualActivity
	            case MSG_REGISTER_CLIENT:
	                mClients.add(msg.replyTo);
	                //
	                servo=true;
	                break;
	             //El caso de que se una LineActivity
	            case MSG_REGISTER_CLIENT2:
	            	mClients.add(msg.replyTo);
	            	//
	            	siguelineas=true;
	            	break;
	            	//El caso en que ManualActivity deje de utilizar el servicio	
	            case MSG_UNREGISTER_CLIENT:
	                mClients.remove(msg.replyTo);
	                servo=false;
	                break;
	                //El caso en que LineActivity deje de utilizar el servicio 
	            case MSG_UNREGISTER_CLIENT2:
	            	mClients.remove(msg.replyTo);
	            	siguelineas=false;
	            	break;
	           
	            default:
	                super.handleMessage(msg);
	            }
	        }
	    }
	 

	    @Override
	    public void onCreate() {
	        super.onCreate();
	        //Asi se escriben mensajes en el LogCat,  es útil para identificar errores y saber hasta donde se ejecuta el código
	        Log.i("MyService", "Service Started.");
	        
	        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

		       
	        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

	        
	        IntentFilter filter = new IntentFilter();
	        filter.addAction(ACTION_USB_PERMISSION);
	        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
	        registerReceiver(mUsbReceiver, filter);
	        
	        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	        
	    
	        
	        
	      
	        
	        
	        
	        
	        
	    }
	    
	    
	    
	
	    @Override
	    public int onStartCommand(Intent intent, int flags, int startId) {
	        Log.i("MyService", "Received start id " + startId + ": " + intent);
	        
	        //se llama a esta función para recibir los datos del acelerómetro del móvil
	        sensorManager.registerListener(sensorEventListener, accelerometer,
	    			 SensorManager.SENSOR_DELAY_GAME);
	        
	        
	        
	      
	        
	       if (mInputStream != null && mOutputStream != null){
	        	 return -1 ;
	         }
	         
	        
	         UsbAccessory[] accessories = mUsbManager.getAccessoryList();
	         UsbAccessory accessory = (accessories == null ? null : accessories[0]);
	         if (accessory != null){
	        	 if (mUsbManager.hasPermission(accessory)){
	        		 openAccessory(accessory);
	        	 }else{
	        		 synchronized (mUsbReceiver) {
	                     if (!mPermissionRequestPending){
	                    	 mUsbManager.requestPermission(accessory, mPermissionIntent);
	                         mPermissionRequestPending = true;
	                     }
	        	 }
	         }
	    }else{
	    	 Log.d(TAG, "mAccessory is null");
	    }
	         //s
	         
	         return START_STICKY; // run until explicitly stopped.
	         
	        
     }
	       
	    

	 


	     

	    @Override
	    public void onDestroy() {
	        
	       
	        closeAccessory();
	        sensorManager.unregisterListener(sensorEventListener);
	        unregisterReceiver(mUsbReceiver);
	        super.onDestroy();
	    }
	    //Esta función es para comunicarse con el arduino
	    private void openAccessory(UsbAccessory accessory){
	    	 mFileDescriptor = mUsbManager.openAccessory(accessory);

	         if (mFileDescriptor != null){
	        	 mAccessory = accessory;
	             FileDescriptor fd = mFileDescriptor.getFileDescriptor();

	            //Esta clase lee los mensajes del buffer del usb
	             mInputStream = new FileInputStream(fd);
	             //Escribe en el buffer del usb
	             mOutputStream = new FileOutputStream(fd);

	             // Se crea un hilo de ejecución paralelo al principal para que no se sature el servicio,
	             // se encargará de recibir todos los mensajes provenientes del arduino .
	             // Se implementa con Runnable, elemento de android que contiene la función run() para manejar todos
	             //los mensajes enviados por arduino
	             Thread thread = new Thread(null, this, "DemoKit");
	             thread.start();
	             Log.d(TAG, "accessory opened");

	             
	         }else{
	        	 Log.d(TAG, "accessory open fail");
	         }
	    }
	    
	    private void closeAccessory(){
	    	 //enableControls(false);
	    	 try{
	    		 if (mFileDescriptor != null){
	    			 mFileDescriptor.close();
	    		 }
	    	 }catch (IOException e){
	    		 
	    	 }finally{
	    		 mFileDescriptor = null;
	             mAccessory = null;
	    	 }
	    }
	    
	   
	    private static final int MESSAGE_LIGHT = 3; 
	    private static final int MESSAGE_ULTRASONIC = 4;
	    private static final int MESSAGE_RIGHT_CONTACT=5;
	    private static final int MESSAGE_LEFT_CONTACT=6;
	    

	    
	    protected class LightMsg {
			private int light;

			public LightMsg(int light) {
				this.light = light;
			}

			public int getLight() {
				return light;
			}
		}
	    
	    protected class UltrasonicMsg{
	    	private int ultrasonic;
	    	
	    	public UltrasonicMsg(int ultrasonic){
	    		this.ultrasonic=ultrasonic;
	    	}
	    	
	    	public int getUltrasonic(){
	    		return ultrasonic;
	    	}
	    }
	    
	    protected class LeftContactMsg{
			private byte value;
			

	        public LeftContactMsg(byte value){
	        	this.value = value;
	        }
	        public byte getValue(){
	        	return value;
	        }
	       
	      
	                
			
		}
	    
	    protected class RightContactMsg{
			private byte value;
			

	        public RightContactMsg(byte value){
	        	this.value= value;
	        }
	       public byte getValue(){
	    	   return value; 
	       }
	                
			
		}
	    	
	    
	    
	    //Función perteneciente al elemento Runnable, implementado en la clase Servicio.Maneja los mensajes
	    //que envía el arduino
	    @Override
	    public void run(){
	    	 int ret = 0;
	         byte[] buffer = new byte[16384];
	         int i;

	       
	         while (ret >= 0){
	        	 try{
	        		 ret = mInputStream.read(buffer);
	        	 }catch(IOException e){
	        		 break;
	        	 }
	        	 i = 0;
	             while (i < ret){
	            	 int len = ret - i;
	            	 switch (buffer[i]){
	            	 
	        
	            		 
	            	 case 0x5:
		 					if (len >= 3) {
		 						Message m = Message.obtain(mHandler, MESSAGE_LIGHT);
		 						m.obj = new LightMsg(composeInt(buffer[i + 1],
		 								buffer[i + 2]));
		 						mHandler.sendMessage(m);
		 					}
		 					i += 3;
		 					break;
		 					
	            	 case 0x7:
	                	 if(len >= 3){
	                		 Message m=Message.obtain(mHandler, MESSAGE_ULTRASONIC);
	                		 m.obj = new UltrasonicMsg(composeInt(buffer[i + 1],
		 								buffer[i + 2]));
		 						mHandler.sendMessage(m);
		 					}
		 					i += 3;
		 					break;
		 					
	            	 case 0x8:
	                	 if(len >= 3){
	                		 Message m=Message.obtain(mHandler, MESSAGE_LEFT_CONTACT);
	                		 m.obj = new LeftContactMsg(buffer[i +1]);
		 								
		 						mHandler.sendMessage(m);
		 					}
		 					i += 3;
		 					break;
	            	 case 0x6:
	                	 if(len >= 3){
	                		 Message m=Message.obtain(mHandler, MESSAGE_RIGHT_CONTACT);
	                		 m.obj = new RightContactMsg(buffer[i + 1]);
		 								
		 						mHandler.sendMessage(m);
		 					}
		 					i += 3;
		 					break;
	            	 case 0x9:
	                	 if(len >= 3){
	                		 Message m=Message.obtain(mHandler, MESSAGE_LEFT_CONTACT);
	                		 m.obj = new LeftContactMsg(buffer[i + 1]);
		 								
		 						mHandler.sendMessage(m);
		 					}
		 					i += 3;
		 					break;
		 					
	            		 default:
	            			 Log.d(TAG, "unknown msg: " + buffer[i]);
	                         i = len;
	                         break;
	                         
	                
	                		 
	                	 }
	            		 
	            	 
	            	 }
	            	 
	             }
	         }
	    
	    
	    
	    private Handler mHandler = new Handler(){
	    	  @Override
	          public void handleMessage(Message msg){
	    		  switch (msg.what){
	    		 
	                    
	    		  case MESSAGE_LIGHT:
		  				LightMsg l = (LightMsg) msg.obj;
		  				//handleLightMessage(MSG_LIGHT,l.light);
		  				handleLightMessage(MSG_LIGHT,l.getLight());
		  				break;
		  				
	    		  case MESSAGE_ULTRASONIC:
		  				UltrasonicMsg u = (UltrasonicMsg) msg.obj;
		  				//handleLightMessage(MSG_LIGHT,l.light);
		  				handleUltrasonicMessage(MSG_ULTRASONIC,u.getUltrasonic());
		  				break;
		  				
	    		  case MESSAGE_LEFT_CONTACT:
		  				LeftContactMsg left = (LeftContactMsg) msg.obj;
		  				
		  				handleContactMessage(MSG_LEFT_CONTACT,left.getValue());
		  				break;
	    		  case MESSAGE_RIGHT_CONTACT:
		  				RightContactMsg right = (RightContactMsg) msg.obj;
		  				
		  				handleContactMessage(MSG_RIGHT_CONTACT,right.getValue());
		  				break;
	    		 
	    		  }
	    	  }
	    };
	    
	  
	    
	    private void handleLightMessage(int ID,int value){
	    	//mando a todas las actividades cliente el dato del sensor de la LDR. en este caso sólo será a 
	    	//ManualActivity o LineActivity
	    	 for (int i=mClients.size()-1; i>=0; i--) {
		            try {
		                  	
		            	// Send data as an Integer
		                mClients.get(i).send(Message.obtain(null, ID, value, 0));
		                
		                
		                

		            } catch (RemoteException e) {
		                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
		                mClients.remove(i);
		            }
		        }
	    	 
	    	 }
	    
	    private void handleUltrasonicMessage(int ID,int value){
	    	
	    	 for (int i=mClients.size()-1; i>=0; i--) {
		            try {
		                  	
		            	// Send data as an Integer
		                mClients.get(i).send(Message.obtain(null, ID, value, 0));
		                
		                
		               

		            } catch (RemoteException e) {
		                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
		                mClients.remove(i);
		            }
		        }
	    	 
	    	 }
	    
	    private void handleContactMessage(int ID, byte value){
	    	
	    	for(int i=mClients.size()-1; i>=0; i--){
	    		try{
	    			//send data as a byte
	    			Bundle b= new Bundle();
	    			b.putByte("b1", value);
	    			Message msg=Message.obtain(null, ID);
	    			msg.setData(b);
	    			mClients.get(i).send(msg);
	    			
	    			
	    		}catch (RemoteException e){
	    			mClients.remove(i);
	    			
	    		}
	    		
	    	}
	    	
	    }

	    
	   
	    
	    
	    
	    public static boolean isRunning()
	    {
	        return isRunning;
	    }
	    
	   //callback para obtener los datos del acelrómetro y enviárselo al arduino 
       private final SensorEventListener sensorEventListener = new SensorEventListener(){
	    	
	    	int x_acceleration;
	    	int y_acceleration;
	    	@Override
	    	public void onAccuracyChanged(Sensor sensor, int accuracy){
	    		
	    	}
	    	@Override
	    	public void onSensorChanged(SensorEvent event){
	    		x_acceleration = (int)(-event.values[0] * 10);
	    		y_acceleration=(int)(-event.values[1]*10);
	    		//mandar el mensaje al arduino del valor del servo
	    			moveServoCommand(SERVO_ID_1, x_acceleration);
		    		moveServoCommand(SERVO_ID_2, y_acceleration);
		    		
	     //aqi hay que poner el codigo para mandar los valores a la actividad
	    		handleMessageServo(MSG_SERVO_1,x_acceleration);
	    		handleMessageServo(MSG_SERVO_2,y_acceleration);
	    		 
	    	
	    	}
	    };
	    
	    public void moveServoCommand(byte target, int value){
	    	byte[] buffer = new byte[6];
	    	if(servo){
	    	buffer[0] = COMMAND_SERVO;
	    	}else if(siguelineas){
	    		buffer[0]=0x1;
	    	}
	    	//En el buffer convierto la variable de tipo int del acelerómetro en 4 variables de tipo byte.
	    	//Esto se explica en la memoria
	    	buffer[1] = target;
	    	buffer[2] = (byte) (value >> 24);
	    	buffer[3] = (byte) (value >> 16);
	    	buffer[4] = (byte) (value >> 8);
	    	buffer[5] = (byte) value;
	    	if (mOutputStream != null){
	    		try{
	    			//escribo en el buffer del usb
	    			mOutputStream.write(buffer);
	    		}catch(IOException e){
	    			Log.e(TAG, "write failed", e);
	    		}
	    	}
	    }
	    
	    //función para mandar los datos del acelerómetro a la actividad cliente.
	    private void handleMessageServo(int ID,int value_servo){
	    	
	    	 for (int i=mClients.size()-1; i>=0; i--) {
		            try {
		            	
		            	// Send data as an Integer
		                mClients.get(i).send(Message.obtain(null, ID, value_servo, 0));
		                
		                
		            

		            } catch (RemoteException e) {
		                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
		                mClients.remove(i);
		            }
		        }
	    	 
	    	 }

	    

}
