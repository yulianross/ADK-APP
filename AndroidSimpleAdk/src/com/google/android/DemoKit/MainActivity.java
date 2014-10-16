package com.example.adk_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class MainActivity extends Activity {
	//Botones de la actividad
	Button btnStart, btnStop, btnManual, btnLine;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//relaciono esta actividad con su parte gráfica escrita en xml, ubicado en la carpeta layout
		setContentView(R.layout.activity_main);
		
		//reconocimiento  de los botones con el xml
		btnStart = (Button)findViewById(R.id.start_service);
		btnStop=(Button)findViewById(R.id.stop_service);
		btnManual=(Button)findViewById(R.id.manual_mode);
		btnLine=(Button)findViewById(R.id.line_mode);
		
		//Invoco a los callbacks de los botones
		btnStart.setOnClickListener(btnStartListener);
	    btnStop.setOnClickListener(btnStopListener);
	    btnManual.setOnClickListener(btnManualListener);
	    btnLine.setOnClickListener(btnLineListener);
	    
	        
		
	}
	//Cada vez que se presiona el botón StartService se ejecuta este callback
	private OnClickListener btnStartListener = new OnClickListener() {
        public void onClick(View v){
            startService(new Intent(MainActivity.this, MyService.class));
        }
    };
  //Cada vez que se presiona el botón StopService se ejecuta este callback
    private OnClickListener btnStopListener = new OnClickListener() {
        public void onClick(View v){
          
            stopService(new Intent(MainActivity.this, MyService.class));
           
        }
    };
  //Cada vez que se presiona el botón Modo siguelíneas se ejecuta este callback
    private OnClickListener btnManualListener= new OnClickListener(){
    	public void onClick(View v){
    	
    			
    		startActivity(new Intent(MainActivity.this, LineActivity.class));
    	
    	}
    };
  //Cada vez que se presiona el botón Modo Manual se ejecuta este callback
    private OnClickListener btnLineListener= new OnClickListener(){
    	public void onClick(View v){
    		
    		
    		startActivity(new Intent(MainActivity.this, ManualActivity.class));
    		
    	}
    };
    
    @Override
    public void onDestroy(){
    //stopService(new Intent(MainActivity.this, MyService.class));
    	super.onDestroy();
    }
    
	
	



}
