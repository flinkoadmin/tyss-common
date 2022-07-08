package com.tyss.optimize.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BeanUtility {

    public static void copyProperties( Object orig, Object dest) throws Exception {
        BeanUtils.copyProperties(orig, dest);
    }

    public static void copyProperties(Object target, Object source, String[] ignoreProperties) throws Exception {
        BeanUtils.copyProperties(source, target, ignoreProperties);
    }

    public static Field[] getAllFieldsArray(Class<?> type) {
        List<Field> fields = new ArrayList();
        Set<String> propertySet = new HashSet();
        getAllFields(fields, type, propertySet);
        return (Field[]) fields.toArray(new Field[fields.size()]);
    }

    public static List<Field> getAllFields(List<Field> fields, Class<?> type, Set<String> propertySet) {
        Field[] var6;
        int var5 = (var6 = type.getDeclaredFields()).length;

        for (int var4 = 0; var4 < var5; ++var4) {
            Field field = var6[var4];
            if (!propertySet.contains(field.getName())) {
                fields.add(field);
                propertySet.add(field.getName());
            }
        }

        if (type.getSuperclass() != null) {
            fields = getAllFields(fields, type.getSuperclass(), propertySet);
        }

        return fields;
    }
}


