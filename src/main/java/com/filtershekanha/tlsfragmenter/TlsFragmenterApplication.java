package com.filtershekanha.tlsfragmenter;

import com.filtershekanha.tlsfragmenter.config.AppConfig;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;

import java.io.PrintStream;

@SpringBootApplication
@EnableConfigurationProperties(AppConfig.class)
public class TlsFragmenterApplication {

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TlsFragmenterApplication.class);
        app.setBanner(new CustomBanner());
        app.setLogStartupInfo(false); // Remove Springboot startup noise
        app.run(args);
    }

    public static class CustomBanner implements Banner {
        @Override
        public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
            out.println();
            out.println("    _______ ____                 __         __               __         ");
            out.println("   / ____(_) / /____  __________/ /_  ___  / /______ _____  / /_  ____ _");
            out.println("  / /_  / / / __/ _ \\/ ___/ ___/ __ \\/ _ \\/ //_/ __ `/ __ \\/ __ \\/ __ `/");
            out.println(" / __/ / / / /_/  __/ /  (__  ) / / /  __/ ,< / /_/ / / / / / / / /_/ / ");
            out.println("/_/   /_/_/\\__/\\___/_/  /____/_/ /_/\\___/_/|_|\\__,_/_/ /_/_/ /_/\\__,_/  ");
            out.println("\t-= TLS Handshake Fragmentation Proxy =-");
            out.println();
        }
    }

}
