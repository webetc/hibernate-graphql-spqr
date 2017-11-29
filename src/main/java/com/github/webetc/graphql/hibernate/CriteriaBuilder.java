package com.github.webetc.graphql.hibernate;

import graphql.language.Field;
import graphql.language.Selection;
import io.leangen.graphql.execution.ResolutionEnvironment;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.AssociationType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import java.util.*;


public class CriteriaBuilder {

    private SessionFactory sessionFactory;

    public CriteriaBuilder(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Criteria buildCriteria(ResolutionEnvironment env, Class cls, List<String> extraFields) {
        Set<String> moreFields = new HashSet<String>();
        if (extraFields != null)
            moreFields.addAll(extraFields);

        ClassMetadata classMetadata = sessionFactory.getClassMetadata(cls);
        String[] propertyNames = classMetadata.getPropertyNames();
        List<Alias> aliases = new ArrayList<Alias>();

        try {
            ProjectionList projectionList = null;

            if (env != null) {
                projectionList = Projections.projectionList();
                Field root = env.dataFetchingEnvironment.getFields().get(0);
                List<Selection> fields = root.getSelectionSet().getSelections();

                aliases.addAll(addProjections(projectionList, null, propertyNames, fields, classMetadata, moreFields));
            }

            if (moreFields.size() > 0) {
                if (projectionList == null)
                    projectionList = Projections.projectionList();

                for (String s : moreFields) {
                    if (!s.startsWith("!"))
                        projectionList.add(Projections.property(s), s);
                }
            }

            Map<String, String> aliasPathMap = new HashMap<String, String>();
            Criteria cr = sessionFactory.getCurrentSession().createCriteria(cls);
            for (Alias a : aliases) {
                cr.createAlias(a.path(), a.alias());
                aliasPathMap.put(a.alias(), a.fullPath());
            }

            if (projectionList != null)
                cr.setProjection(projectionList);

            cr.setResultTransformer(new POJOResultsTransformer(cls, aliasPathMap));

            return cr;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private List<Alias> addProjections(
            ProjectionList projectionList,
            Alias root,
            String[] propertyNames,
            List<Selection> fields,
            ClassMetadata classMetadata,
            Collection<String> moreFields
    ) {
        List<Alias> aliases = new ArrayList<Alias>();
        String id = "";
        if (classMetadata != null)
            id = classMetadata.getIdentifierPropertyName();

        for (Selection s : fields) {
            Field f = (Field) s;
            boolean validField = f.getName().equals(id);
            for (String prop : propertyNames) {
                if (prop.equals(f.getName()))
                    validField = true;
            }
            if (moreFields.contains("!" + f.getName()))
                validField = false;
            if (validField) {
                Alias alias = new Alias(root, f.getName());
                String fieldName = alias.path();
                String aliasName = alias.alias();
                if (f.getChildren() == null || f.getChildren().size() == 0) {
                    projectionList.add(Projections.property(fieldName), fieldName);
                    moreFields.remove(fieldName);
                } else if (classMetadata != null) {
                    // Handle nested class properties
                    Type t = classMetadata.getPropertyType(f.getName());
                    if (t.isComponentType()) {
                        alias.setAsComponent();
                        ComponentType ct = (ComponentType) t;
                        String[] subPropertyNames = ct.getPropertyNames();
                        List<Selection> subFields = f.getSelectionSet().getSelections();
                        addProjections(projectionList, alias, subPropertyNames, subFields, null, moreFields);
                    } else if (t.isEntityType()) {
                        aliases.add(alias);
                        AssociationType ct = (AssociationType) t;
                        Class cls = ct.getReturnedClass();
                        ClassMetadata subClassMetadata = sessionFactory.getClassMetadata(cls);
                        String[] subPropertyNames = subClassMetadata.getPropertyNames();
                        List<Selection> subFields = f.getSelectionSet().getSelections();
                        aliases.addAll(addProjections(projectionList, alias, subPropertyNames, subFields, subClassMetadata, moreFields));
                    }
                }
            }
        }

        return aliases;
    }


    private class Alias {
        Alias parent;
        String field;
        private boolean component = false;

        Alias(Alias parent, String field) {
            this.parent = parent;
            this.field = field;
        }

        void setAsComponent() {
            this.component = true;
        }

        String path() {
            if (parent == null)
                return field;
            else
                return parent.alias() + "." + field;
        }

        String alias() {
            if (parent == null)
                return field;
            else if (component)
                return path();
            else
                return parent.alias() + "_" + field;
        }

        String fullPath() {
            if (parent == null)
                return field;
            else
                return parent.fullPath() + "." + field;
        }
    }
}

