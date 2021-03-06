package com.wizzdi.dynamic.annotations.service.cn.xdean.jex;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ReflectUtil {

  private static final UnaryOperator<Executable> EXECUTABLE_GET_ROOT;
  private static final Function<Class<?>, Method[]> CLASS_GET_ROOT_METHODS;
  static {
    try {
      Method getRootMethod = Method.class.getDeclaredMethod("getRoot");
      getRootMethod.setAccessible(true);
      EXECUTABLE_GET_ROOT = m -> ExceptionUtil.uncheck(() -> (Executable) getRootMethod.invoke(m));
      Method getRootMethods = Class.class.getDeclaredMethod("privateGetPublicMethods");
      getRootMethods.setAccessible(true);
      CLASS_GET_ROOT_METHODS = c -> ExceptionUtil.uncheck(() -> (Method[]) getRootMethods.invoke(c));

    } catch (NoSuchMethodException | SecurityException  e) {
      throw new IllegalStateException("ReflectUtil init fail, check your java version.", e);
    }
  }

  public static void setModifiers(AccessibleObject ao, int modifier) {
    Arrays.stream(getAllFields(ao.getClass()))
        .filter(f -> f.getName().equals("modifiers"))
        .peek(f -> f.setAccessible(true))
        .forEach(f -> ExceptionUtil.uncheck(() -> f.set(ao, modifier)));
  }

  /**
   * Get root of the method.
   */
  public static Method getRootMethod(Method m) {
    return getRootExecutable(m);
  }

  /**
   * Get root of the executable.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Executable> T getRootExecutable(T m) {
    return (T) EXECUTABLE_GET_ROOT.apply(m);
  }

  /**
   * Get root public methods of the class.
   */
  public static Method[] getRootMethods(Class<?> clz) {
    return CLASS_GET_ROOT_METHODS.apply(clz);
  }



  /**
   * Get field value by name
   *
   * @param clz
   * @param t
   * @param fieldName
   * @return
   * @throws NoSuchFieldException
   */
  @SuppressWarnings("unchecked")
  public static <T, O> O getFieldValue(Class<T> clz, T t, String fieldName) throws NoSuchFieldException {
    Field field = clz.getDeclaredField(fieldName);
    field.setAccessible(true);
    try {
      return (O) field.get(t);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Get all fields
   *
   * @param clz
   * @param includeStatic include static fields or not
   * @return
   */
  public static Field[] getAllFields(Class<?> clz, boolean includeStatic) {
    return Arrays.stream(getAllFields(clz))
        .filter(f -> includeStatic || !Modifier.isStatic(f.getModifiers()))
        .toArray(Field[]::new);
  }

  /**
   * Get all fields
   *
   * @param clz
   * @return
   */
  public static Field[] getAllFields(Class<?> clz) {
    List<Field> list = new ArrayList<>();
    do {
      list.addAll(Arrays.asList(clz.getDeclaredFields()));
    } while ((clz = clz.getSuperclass()) != null);
    return list.toArray(new Field[list.size()]);
  }

  /**
   * Get all methods
   *
   * @param clz
   * @return
   */
  public static Method[] getAllMethods(Class<?> clz) {
    Set<Method> set = new HashSet<>();
    List<Class<?>> classes = new ArrayList<>();
    classes.add(clz);
    classes.addAll(Arrays.asList(getAllSuperClasses(clz)));
    classes.addAll(Arrays.asList(getAllInterfaces(clz)));
    for (Class<?> c : classes) {
      set.addAll(Arrays.asList(c.getDeclaredMethods()));
    }
    return set.toArray(new Method[set.size()]);
  }

  /**
   * Get all super classes
   *
   * @param clz
   * @return
   */
  public static Class<?>[] getAllSuperClasses(Class<?> clz) {
    List<Class<?>> list = new ArrayList<>();
    while ((clz = clz.getSuperclass()) != null) {
      list.add(clz);
    }
    return list.toArray(new Class<?>[list.size()]);
  }

  /**
   * Get all interfaces
   *
   * @param clz
   * @return
   */
  public static Class<?>[] getAllInterfaces(Class<?> clz) {
    HashSet<Class<?>> set = new HashSet<>();
    getAllInterfaces(clz, set);
    return set.toArray(new Class<?>[set.size()]);
  }

  private static void getAllInterfaces(Class<?> clz, Set<Class<?>> visited) {
    if (clz.getSuperclass() != null) {
      getAllInterfaces(clz.getSuperclass(), visited);
    }
    for (Class<?> c : clz.getInterfaces()) {
      if (visited.add(c)) {
        getAllInterfaces(c, visited);
      }
    }
  }

  /**
   * Get the name of the class who calls the caller.<br>
   * For example. The following code will print "A".
   *
   * <pre>
   * <code>class A {
   *   static void fun() {
   *     B.fun();
   *   }
   * }
   *
   * class B {
   *   static void fun() {
   *     System.out.println(ReflectUtil.getCallerClassName());
   *   }
   * }</code>
   * </pre>
   *
   * @return
   */
  public static String getCallerClassName() {
    return getCaller().getClassName();
  }

  public static StackTraceElement getCaller() {
    return getCaller(1, true);
  }

  /**
   * Get caller stack info.
   *
   * @param deep Deep to search the caller class.If deep is 0, it returns the class who calls this
   *          method.
   * @param ignoreSameClass If it is true, calling in same class will be ignored.
   */
  public static StackTraceElement getCaller(int deep, boolean ignoreSameClass) {
    // index 0 is Thread.getStackTrace
    // index 1 is ReflectUtil.getCallerClassName
    StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
    StackTraceElement currentStack = stElements[1];
    int found = deep + 1;
    for (int i = 1; i < stElements.length; i++) {
      StackTraceElement nextStack = stElements[i];
      if (nextStack.getClassName().equals(ReflectUtil.class.getName())) {
        continue;
      }
      if (!ignoreSameClass || !currentStack.getClassName().equals(nextStack.getClassName())) {
        currentStack = nextStack;
        found--;
      }
      if (found == 0) {
        return currentStack;
      }
    }
    throw new IllegalArgumentException("Stack don't have such deep.");
  }
}