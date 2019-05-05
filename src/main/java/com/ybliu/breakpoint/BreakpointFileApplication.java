package com.ybliu.breakpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * @author linlinyeyu
 */
@SpringBootApplication
public class BreakpointFileApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(BreakpointFileApplication.class,args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(BreakpointFileApplication.class);
    }
}
