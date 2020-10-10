package com.test.spring;

/**
 * beanDefinition实体类
 */
public class MyBeanDefinition {
    /**bean对应的class对象**/
    private Class beanClass;

    /**bean的生命周期枚举类**/
    private ScopeEnum scope;

    public Class getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }


    public ScopeEnum getScope() {
        return scope;
    }

    public void setScope(ScopeEnum scope) {
        this.scope = scope;
    }
}
