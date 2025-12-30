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
        SpringApplication.run(CivifyApplication.class, args);
        logLogo();
    }

    private static void logLogo() {
        String ridersLogo
                = "\n ######  #### ##     ## #### ######## ##    ##\r\n"
                + "##    ##  ##  ##     ##  ##  ##        ##  ## \r\n"
                + "##        ##  ##     ##  ##  ##         ####  \r\n"
                + "##        ##  ##     ##  ##  ######      ##   \r\n"
                + "##        ##   ##   ##   ##  ##          ##   \r\n"
                + "##    ##  ##    ## ##    ##  ##          ##   \r\n"
                + " ######  ####    ###    #### ##          ##   ";

        log.info(ridersLogo);
    }

}
