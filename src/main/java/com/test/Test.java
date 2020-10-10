package com.test;

import com.test.spring.MyApplicationContext;

public class Test {
    public static void main(String[] args) {
         MyApplicationContext myApplicationContext = new MyApplicationContext(AppConfig.class);


        System.out.println(myApplicationContext.getBean("userService"));
    }
}
