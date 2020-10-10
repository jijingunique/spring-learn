package com.test.spring;

public interface MyBeanPostProcessor {

    void postProcessAfterInitialization(String beanName, Object instance);
}
