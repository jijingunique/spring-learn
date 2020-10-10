package com.test.service;

import com.test.spring.MyBeanPostProcessor;

/**
 * 自定义后置处理器
 */
public class TestBeanPostProcessor implements MyBeanPostProcessor {

    @Override
    public void postProcessAfterInitialization(String beanName, Object instance) {
        System.out.println("..TestBeanPostProcessor执行了");
    }
}
