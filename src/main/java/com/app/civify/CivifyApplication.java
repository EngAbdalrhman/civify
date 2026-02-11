package com.app.civify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details Main class for the application
 * @since 1.0.0
 * @version 1.0.0
 * @date 2025-10-16
 *
 */
@EnableScheduling
@EnableAsync
@SpringBootApplication
@EnableFeignClients
@EnableTransactionManagement
@Slf4j
public class CivifyApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CivifyApplication.class);
        app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
        app.run(args);
        logLogo();
    }

    private static void logLogo() {
        String civifyLogo = """
                  
                                                                                                                            
                                                                                                           
        CCCCCCCCCCCCC  iiii                            iiii     ffffffffffffffff                           
     CCC::::::::::::C i::::i                          i::::i   f::::::::::::::::f                          
   CC:::::::::::::::C  iiii                            iiii   f::::::::::::::::::f                         
  C:::::CCCCCCCC::::C                                         f::::::fffffff:::::f                         
 C:::::C       CCCCCCiiiiiiivvvvvvv           vvvvvvviiiiiii  f:::::f       ffffffyyyyyyy           yyyyyyy
C:::::C              i:::::i v:::::v         v:::::v i:::::i  f:::::f              y:::::y         y:::::y 
C:::::C               i::::i  v:::::v       v:::::v   i::::i f:::::::ffffff         y:::::y       y:::::y  
C:::::C               i::::i   v:::::v     v:::::v    i::::i f::::::::::::f          y:::::y     y:::::y   
C:::::C               i::::i    v:::::v   v:::::v     i::::i f::::::::::::f           y:::::y   y:::::y    
C:::::C               i::::i     v:::::v v:::::v      i::::i f:::::::ffffff            y:::::y y:::::y     
C:::::C               i::::i      v:::::v:::::v       i::::i  f:::::f                   y:::::y:::::y      
 C:::::C       CCCCCC i::::i       v:::::::::v        i::::i  f:::::f                    y:::::::::y       
  C:::::CCCCCCCC::::Ci::::::i       v:::::::v        i::::::if:::::::f                    y:::::::y        
   CC:::::::::::::::Ci::::::i        v:::::v         i::::::if:::::::f                     y:::::y         
     CCC::::::::::::Ci::::::i         v:::v          i::::::if:::::::f                    y:::::y          
        CCCCCCCCCCCCCiiiiiiii          vvv           iiiiiiiifffffffff                   y:::::y           
                                                                                        y:::::y            
                                                                                       y:::::y             
                                                                                      y:::::y              
                                                                                     y:::::y  
                  """;

        log.info(civifyLogo);
    }

}
