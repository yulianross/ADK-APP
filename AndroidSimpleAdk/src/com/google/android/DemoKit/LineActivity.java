package com.example.adk_app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class LineActivity extends Activity {
	
	Button btnBind, btnUnbind;
	TextView textRightContact, textLeftContact, textLight, textUltrasonic, textServo_x, textServo_y;
	
	Messenger mService = null;
	boolean mIsBound;
	
	 private Vibrator vibrator;
	 private boolean isVibrating=false;
	
	
	//lo utilizo para comunicarme con el Service. Aqui recibo lo que el service me manda.
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    
    //La clase IncomingHandler maneja los mensajes recibidos, y según que mensaje sea actúa en consecuencia
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            
            case MyService.MSG_LIGHT:
            	//String light_value=Integer.toString(msg.getData().getInt("int1"));
            	textLight.setText("Iluminación: "+ msg.arg1); 
            	break;
            
            case MyService.MSG_ULTRASONIC:
            	textUltrasonic.setText("ultrasonidos: " + msg.arg1 + "cm");
               break;
                
            case MyService.MSG_SERVO_1:
            	 textServo_x.setText("Eje x : " + msg.arg1);
            	 break;
            	 
            case MyService.MSG_SERVO_2:
            	textServo_y.setText("Eje y : " + msg.arg1);
            	break;
            	
            case MyService.MSG_LEFT_CONTACT:
            	LeftContactMsg LeftContact=new LeftContactMsg( msg.getData().getByte("b1"));
            	textLeftContact.setText(LeftContact.isPressed());
    	    	LeftContact.isVibrate();
            	break;
            	
            case MyService.MSG_RIGHT_CONTACT:
            	//byte RightContactValue=msg.getData().getByte("b1");
            	RightContactMsg RightContact= new RightContactMsg(msg.getData().getByte("b1"));
            	textRightContact.setText(RightContact.isPressed());
            	RightContact.isVibrate();
            	break;
            	
            
            	
            	
            	
            	
            default:
                super.handleMessage(msg);
            }
        }
    }
    
 private ServiceConnection mConnection = new ServiceConnection() {
    	
    	//5. Crea un Messenger y despues se lo asigna a un messenger para que lo envie al service
    	
        public void onServiceConnected(ComponentName className, IBinder service) {
        	// mService es de tipo Messenger, para mandar mensajes al servicio. toma como parámetro service de
        	//la clase Ibinder, que contiene información sobr la identificación del Servicio.
            mService = new Messenger(service);
           
            try {
            	//MSG_REGISTER_CLIENT2 es la constante que identifica a la clase cliente LineActivity
                Message msg = Message.obtain(null, MyService.MSG_REGISTER_CLIENT2);
                msg.replyTo = mMessenger;
                //aqui mando el mensaje al Service. el mMessenger del servicio recibirá este mensaje
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
           
        }
    };
    
    //En la función onCreate() se crean e inicializan objetos
    @Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    	//esta línea es para asignar la parte gráfica de la actividad (el archivo xml de la actividad)
    	setContentView(R.layout.activity_line);
    	//Relaciono todos los botones y textViews con sus archivos xlm (su parte gráfica)
    	btnBind=(Button)findViewById(R.id.btnBind);
    	btnUnbind=(Button)findViewById(R.id.btnUnbind);
    	textRightContact=(TextView)findViewById(R.id.right_contact);
    	textLeftContact=(TextView)findViewById(R.id.left_contact);
    	textLight=(TextView)findViewById(R.id.light_text);
    	textUltrasonic=(TextView)findViewById(R.id.ultrasonic_text);
    	textServo_x=(TextView)findViewById(R.id.x_text);
    	textServo_y=(TextView)findViewById(R.id.y_text);
    	//Con getSystemService puedo utlizar el vibrador del móvil.
    	vibrator = ((Vibrator) getSystemService(VIBRATOR_SERVICE));
    	
 // 3. Llamo a la funcion setOnclickListener() y le paso como referencia la función del paso 2.
        
        btnBind.setOnClickListener(btnBindListener);
        btnUnbind.setOnClickListener(btnUnbindListener);
        
        CheckIfServiceIsRunning(); 
        
    	
    	
    	
    }
    
    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (MyService.isRunning()) {
            doBindService();
        }
    }
    
 // 1. este callback se ejecuta cuando presiono el boton "bind to the Service". esta funcion la llamaré
    // en onCreate().
    private OnClickListener btnBindListener = new OnClickListener() {
        public void onClick(View v){
            doBindService();
        }
    };
    private OnClickListener btnUnbindListener = new OnClickListener() {
        public void onClick(View v){
            doUnbindService();
        }
    };
    
// 2. esta funcion la utilizo para unirme al servicio. Además pongo una variable a "true"
    
    void doBindService() {
        bindService(new Intent(this, MyService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        //textStatus.setText("Binding.");
    }
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_UNREGISTER_CLIENT2);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            //textStatus.setText("Unbinding.");
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            doUnbindService();
            stopVibrate();
        } catch (Throwable t) {
            Log.e("MainActivity", "Failed to unbind from the service", t);
        }
    }
	
    //Clase utilizada en el IncomingHandler, en el caso de los sensores de contacto 
    protected class RightContactMsg{
		private byte value;
		String pressed="choque";
		String not_pressed="No hay contacto";

        public RightContactMsg(byte value){
        	this.value = value;
        }
        public String isPressed(){
        	if(value == 0){
        		return pressed;
        		
        	}else{
        		return not_pressed;
        	}
        }
        public void isVibrate(){
        	if(value == 0){
        		startVibrate();
        		
        	}else{
        		stopVibrate();
        	}
        }
                
		
	}
    
    protected class LeftContactMsg{
		private byte value;
		String pressed="choque";
		String not_pressed="No hay contacto";

        public LeftContactMsg(byte value){
        	this.value = value;
        }
        public String isPressed(){
        	if(value == 0){
        		return pressed;
        		
        	}else{
        		return not_pressed;
        	}
        }
        
        public void isVibrate(){
        	if(value == 0){
        		startVibrate();
        		
        	}else{
        		stopVibrate();
        	}
        }
                
		
	}
  //Función utilizada en las clases  RightContactMsg y LeftContactMsg. Así desactivo el vibrador del móvil
    public void stopVibrate(){
    	if(vibrator!=null && isVibrating){
    		isVibrating=false;
    		vibrator.cancel();
    	}
    }
    
    //Función utilizada en las clases  RightContactMsg y LeftContactMsg. Así activo el vibrador del móvil
    public void startVibrate(){
    	if(vibrator!=null && !isVibrating){
    		isVibrating=true;
			vibrator.vibrate(new long[]{0,100,250},0);
			
    	}
    }
  

}
