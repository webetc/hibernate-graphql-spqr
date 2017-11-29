package com.github.webetc.graphql.hibernate;

import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.HibernateException;
import org.hibernate.transform.ResultTransformer;

import java.util.List;
import java.util.Map;

public class POJOResultsTransformer implements ResultTransformer {
    private final Class resultClass;
    private Map<String, String> aliasPathMap;

    POJOResultsTransformer(Class resultClass, Map<String, String> aliasPathMap) {
        if (resultClass == null) {
            throw new IllegalArgumentException("resultClass cannot be null");
        } else {
            this.resultClass = resultClass;
        }
        this.aliasPathMap = aliasPathMap;
    }

    public Object transformTuple(Object[] tuple, String[] aliases) {
        try {

            Object result = this.resultClass.newInstance();

            for (int i = 0; i < aliases.length; ++i) {
                String alias = aliases[i];
                String[] aliasParts = alias.split("\\.");
                if (aliasParts.length > 1 && aliasPathMap != null) {
                    String fullPath = aliasPathMap.get(aliasParts[0]);
                    if (fullPath != null) {
                        alias = fullPath;
                        for (int j = 1; j < aliasParts.length; j++)
                            alias += "." + aliasParts[j];
                    }
                }
                BeanUtils.setProperty(result, alias, tuple[i]);
            }

            return result;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new HibernateException("Could not instantiate resultclass: " + this.resultClass.getName());
        } catch (Exception e) {
            e.printStackTrace();
            throw new HibernateException(e.getMessage());
        }
    }

    public List transformList(List collection) {
        return collection;
    }

}
