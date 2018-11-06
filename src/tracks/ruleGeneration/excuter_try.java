package tracks.ruleGeneration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class excuter_try {

  public static void main(String[] args) {
    
    System.out.println("Entering main");
    Set<String> events = new HashSet<String>();
    events.add("epe");
    events.add("epe");
    System.out.println(events);
    ExecutorService exec = Executors.newFixedThreadPool(3);
    
    try {
      for(int i=0;i<10; i++){
        exec.execute(new Runnable(){
          public void run() {
            try {
              Thread.sleep(1 * 1000);
              System.out.println("Hello! - " 
                + Thread.currentThread().getId());
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        });
      }
      
      System.out.println("Sleeping..." 
        + Thread.currentThread().getId());
      Thread.sleep(15 * 1000);
      
    } catch(InterruptedException e) {
      e.printStackTrace();
    }

    System.out.println("Exit main");
    
  }

}