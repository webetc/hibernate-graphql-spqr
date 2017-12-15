package com.github.webetc.graphql.hibernate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.leangen.graphql.metadata.strategy.query.BeanResolverBuilder;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.function.Predicate;

public class JsonIgnoreBeanResolverBuilder extends BeanResolverBuilder {

    public static class NotIgnored implements Predicate<Member> {

        @Override
        public boolean test(Member member) {
            if (Method.class.isInstance(member)) {
                Method method = (Method) member;
                return !method.isAnnotationPresent(JsonIgnore.class);
            }
            return true;
        }
    }

    public JsonIgnoreBeanResolverBuilder(String basePackage) {
        super(basePackage);
        this.withFilters(new NotIgnored());
    }
}
