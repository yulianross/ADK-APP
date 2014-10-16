#include <Wire.h>
#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include<Servo.h>

#define LIGHT_SENSOR A1
#define SERVO1 3
#define SERVO2 2
#define BUTTON1 A4
#define BUTTON2 A5

Servo servos[2];
//sensor de ultrasonidos
const int trigger = 8;
const int echo = 7;
const unsigned long timeOut = 5884;//Tiempo de espera máximo (para 100cm). Cálculo: timeOut = ((distancia máxima(cm)/0.017)+1
float distancia_centimetros;
//
unsigned long duracion_microsegundos;

byte b1,b2;
//variables globales para los servos
int posInDegrees_x;
int posInDegrees_y;
int rueda_der;
int rueda_izq;
//variables globales para los CNY70
int iriPin= 10;
int irdPin= 11;
int irderecho,irizquierdo;
 //uint16_t val,val2;
 //float value;
 boolean contact;
 


AndroidAccessory acc("uPhyca",
"HelloADK",
"DemoKit Arduino Board",
"1.0",
"http://www.android.com",
"0000000012345678");

void setup();
void loop();


void init_buttons(){
  
  pinMode(BUTTON1,INPUT);
  pinMode(BUTTON2,INPUT);
  //digitalWrite(BUTTON_1,HIGH);
   b1=digitalRead(BUTTON1);
   b2=digitalRead(BUTTON2);
}

void init_pins(){
  pinMode(iriPin,INPUT);
  pinMode(irdPin,INPUT);
}


void setup()
{
    Serial.begin(115200); // 115200 bits por segundo
    Serial.print("\r\nStart");
    servos[0].attach(SERVO1);
    servos[0].write(90);
    servos[1].attach(SERVO2);
    servos[1].write(90);
    contact=false;
    
    
    pinMode(trigger,OUTPUT);
    pinMode(echo,INPUT);
    init_buttons();
    init_pins();

    acc.powerOn();
}



void loop()
{
    byte msg[3];
    static byte count = 0;
    byte b,c;
    
    
    if (acc.isConnected()) {
        uint16_t val,val2;
        float value;
        
        int len = acc.read(msg, sizeof(msg), 1);

        if (len > 0) {
          
           if(contact){
              servos[0].write(90);
              servos[1].write(90);
           }
           
          
           if(!contact ){
                if(msg[0]==0x7){
                  if(msg[1]==0x1){
                     posInDegrees_x=((msg[2] & 0xFF) << 24)
                                       +((msg[3] & 0xFF) << 16)
                                       +((msg[4] & 0xFF) << 8)
                                       +((msg[5] & 0xFF));
                     rueda_der=map(posInDegrees_x,-100,100,78,102);
                     rueda_izq=map(posInDegrees_x,-100,100,102,78);
                     
                  }
                   
                  
                  if(msg[1]==0x2){
                    posInDegrees_y=((msg[2] & 0xFF) << 24)
                                       +((msg[3] & 0xFF) << 16)
                                       +((msg[4] & 0xFF) << 8)
                                       +((msg[5] & 0xFF));
                    posInDegrees_y=map(posInDegrees_y,-100,100,78,102);
                    
                    
                   
                   }
                   
                   //así giro el robot hacia la derecha
                   if(posInDegrees_y <= 80){
                     //paro rueda derecha
                     rueda_der=78;
                     //rueda izquierda al máximo
                     rueda_izq=90;
                   }
                   //giro el robot hacia la izquierda
                   if(posInDegrees_y >= 92){
                     //paro rueda izquierda
                     rueda_izq= 102;
                     //rueda derecha al máximo
                     rueda_der=90;
                   }
                   
                   servos[0].write(rueda_der);
                   servos[1].write(rueda_izq);
                } 
                   
                   
                
                    
            
         if(msg[0]==0x1){
          
          irderecho=digitalRead(irdPin);
          irizquierdo=digitalRead(iriPin);
          
          if(irderecho==0 && irizquierdo==0){
            rueda_der=78;
            rueda_izq=102;
          }
          if(irderecho==1 && irizquierdo==1){
            rueda_der=90;
            rueda_izq=90;
          }
          if(irderecho==0 && irizquierdo==1){
            rueda_der=90;
            rueda_izq=102;
          }
          if(irderecho==1 && irizquierdo==0){
            rueda_der=78;
            rueda_izq=90;
          }
          
          servos[0].write(rueda_der);
          servos[1].write(rueda_izq);
        
      
        } 
       }
    
       
       contact=false;     
     } 
      
          b = digitalRead(BUTTON1);
          //Serial.println(b);
          if(b==1){
            contact=true;
          }
            
          
          
		if (b != b1) {
			msg[0] = 0x6;
			msg[1] = b ? 0 : 1;
			acc.write(msg, 3);
			b1 = b;
		}
          c= digitalRead(BUTTON2);
          if(c==1){
            contact=true;
          }
            
          
        if(c!=b2){
          msg[0]=0x9;
          msg[1]=c ? 0: 1;
          acc.write(msg,3);
          b2=c;
        }
        
        
        
          
        
        
        switch (count++ % 0x10) {
		

		case 0:
			val = analogRead(LIGHT_SENSOR);
			msg[0] = 0x5;
			msg[1] = val >> 8;
			msg[2] = val & 0xff;
			acc.write(msg, 3);
			break;

                case 0x4:
                        //digitalWrite(trigger,LOW);
                        //delayMicroseconds(3);//en principio estas dos lineas no hacen falta, pero las ponemos por seguridad
                        digitalWrite(trigger,HIGH);
                        delayMicroseconds(10);//tiempo de activación de disparo mínimo del trigger del HC-SR04 según fabricante
                        digitalWrite(trigger,LOW);
                        duracion_microsegundos = pulseIn(echo,HIGH,timeOut);
                        value= conversion(duracion_microsegundos);
                        //hago un cast, convierto value de float a uint16_t
                        val2=( uint16_t) value;
                        //Serial.println(val2);
                        msg[0]=0x7;
                        msg[1]=val2>> 8;
                        msg[2]=val2 & 0xff;
                        acc.write(msg,3);
                        break;
                        
  

        } 
     
        } else{
        servos[0].write(90);
        servos[1].write(90);

    delay(5);
}
}
//=================================================================================================================================================================================00

float conversion(unsigned long duracion){
  float distancia;
  distancia = duracion/58.2;// vel_sonido = 340 m/s = 0.034 cm/us --> dist(cm)=(t/2)*0.034=t*0.017
  return distancia;
}






