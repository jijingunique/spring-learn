package com.test.spring;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 容器上下文类
 */
public class MyApplicationContext {
    private Map<String,MyBeanDefinition> beanDefinitionMap = Maps.newHashMap();

    /**存放单例bean信息**/
    private Map<String,Object> singletonObjects = Maps.newHashMap();

    /**存放bean的后置处理器信息**/
    private List<MyBeanPostProcessor> beanPostProcessorList = Lists.newArrayList();

    /**
     * 通过构造方法加载非加载的单例bean
     * @param configClass  配置类
     */
    public MyApplicationContext(Class configClass) {
        //扫描配置类，并实例化beanDefinition
        scan(configClass);

        //实例化非懒加载的单例Bean
        instanceSingletonBean();
    }

    /**
     * 实例化非懒加载的单例Bean
     */
    private void instanceSingletonBean() {
        for (String beanName : beanDefinitionMap.keySet()) {
            MyBeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

            if (beanDefinition.getScope().equals(ScopeEnum.singleton)) {
                Object bean = doCreateBean(beanName, beanDefinition);

                singletonObjects.put(beanName, bean);
            }
        }
    }

    /**
     * 创建bean
     * @param beanName
     * @param beanDefinition
     * @return
     */
    private Object doCreateBean(String beanName, MyBeanDefinition beanDefinition) {
        Class beanClass = beanDefinition.getBeanClass();

        try {
            // 实例化
            Constructor declaredConstructor = beanClass.getDeclaredConstructor();
            Object instance = declaredConstructor.newInstance();

            // 填充属性
            Field[] fields = beanClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    String fieldName = field.getName();
                    Object bean = getBean(fieldName);

                    field.setAccessible(true);
                    field.set(instance, bean);
                }
            }

            // Aware回调
            if (instance instanceof MyBeanNameAware) {
                ((MyBeanNameAware)instance).setBeanName(beanName);
            }

            // 初始化
            if (instance instanceof InitializingBean) {
                ((InitializingBean)instance).afterPropertiesSet();
            }

            for (MyBeanPostProcessor beanPostProcessor: beanPostProcessorList) {
                beanPostProcessor.postProcessAfterInitialization(beanName, instance);
            }

            return instance;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Object getBean(String beanName) {

        if (singletonObjects.containsKey(beanName)) {
            return singletonObjects.get(beanName);
        } else {
            MyBeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            return doCreateBean(beanName, beanDefinition);
        }
    }

    /**
     * 扫描配置类，并实例化beanDefinition
     * @param configClass 配置类
     */
    private void scan(Class configClass) {
        //获取配置类上的指定注解
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
        //扫描的包
        String basePath = componentScanAnnotation.value();
        // 扫描包路径得到classList
        List<Class> classList = genBeanClasses(basePath);

        // 遍历class得到BeanDefinition
        for (Class clazz : classList) {
            if (clazz.isAnnotationPresent(Component.class)) {
                MyBeanDefinition beanDefinition = new MyBeanDefinition();
                beanDefinition.setBeanClass(clazz);

                // 要么Spring自动生成，要么从Component注解上获取
                Component component = (Component) clazz.getAnnotation(Component.class);
                String beanName = component.value();

                if (MyBeanPostProcessor.class.isAssignableFrom(clazz)) {
                    try {
                        MyBeanPostProcessor instance = (MyBeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
                        beanPostProcessorList.add(instance);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }

                // 解析scope
                if (clazz.isAnnotationPresent(Scope.class)) {
                    Scope scope = (Scope) clazz.getAnnotation(Scope.class);
                    String scopeValue = scope.value();
                    if (ScopeEnum.singleton.name().equals(scopeValue)) {
                        beanDefinition.setScope(ScopeEnum.singleton);
                    } else {
                        beanDefinition.setScope(ScopeEnum.prototype);
                    }
                } else {
                    beanDefinition.setScope(ScopeEnum.singleton);
                }

                beanDefinitionMap.put(beanName, beanDefinition);
            }
        }

    }

    /**
     * 获取指定路径下的所有类信息
     * @param basePath
     * @return
     */
    private List<Class> genBeanClasses(String basePath) {
        ArrayList<Class> classList = Lists.newArrayList();
        ClassLoader classLoader = MyApplicationContext.class.getClassLoader();
        List<File> fileList = listAllFile(basePath);

        fileList.forEach(s->{
            String fileName = s.getAbsolutePath();
            if( fileName.endsWith(".class")){
                String className = fileName.substring(fileName.indexOf("com"),  fileName.indexOf(".class"));
                className = className.replace("\\", ".");
                //                    System.out.println(className);
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    classList.add(clazz);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });



        return classList;
    }

    /**
     * 递归遍历，获取所有class文件
     * @param basePath
     * @return
     */
    private List<File> listAllFile(String basePath) {
        List<File> list = Lists.newArrayList();
        ClassLoader classLoader = MyApplicationContext.class.getClassLoader();
        basePath = basePath.replace(".", "/");
        URL resource = classLoader.getResource(basePath);

        return  getAllFiles(resource.getFile());
    }

    private List<File> getAllFiles(String path){
        List<File> list = Lists.newArrayList();
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (null != files) {
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                        System.out.println("文件夹:" + file2.getAbsolutePath());
                        getAllFiles(file2.getAbsolutePath());
                    } else {
                        list.add(file2);
                        System.out.println("文件:" + file2.getAbsolutePath());
                    }
                }
            }
        }

        return list;
    }

}
