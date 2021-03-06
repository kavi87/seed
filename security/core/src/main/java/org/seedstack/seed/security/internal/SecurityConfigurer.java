/**
 * Copyright (c) 2013-2015, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.security.internal;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.shiro.util.CollectionUtils;
import org.seedstack.seed.security.PrincipalCustomizer;
import org.seedstack.seed.security.Realm;
import org.seedstack.seed.security.RoleMapping;
import org.seedstack.seed.security.RolePermissionResolver;
import org.seedstack.seed.security.internal.authorization.ConfigurationRoleMapping;
import org.seedstack.seed.security.internal.authorization.ConfigurationRolePermissionResolver;
import org.seedstack.seed.security.internal.authorization.EmptyRolePermissionResolver;
import org.seedstack.seed.security.internal.authorization.SameRoleMapping;
import org.seedstack.seed.security.internal.realms.ConfigurationRealm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class SecurityConfigurer {
    private static final String REALMS_KEY = "realms";
    private static final String ROLE_MAPPING_KEY = ".role-mapping";
    private static final String ROLE_PERMISSION_RESOLVER_KEY = ".role-permission-resolver";

    private static final Class<? extends Realm> DEFAULT_REALM = ConfigurationRealm.class;

    private static final Class<? extends RoleMapping> DEFAULT_ROLE_MAPPING = SameRoleMapping.class;
    private static final Class<? extends RoleMapping> CONFIGURATION_ROLE_MAPPING = ConfigurationRoleMapping.class;

    private static final Class<? extends RolePermissionResolver> DEFAULT_ROLE_PERMISSION_RESOLVER = EmptyRolePermissionResolver.class;
    private static final Class<? extends RolePermissionResolver> CONFIGURATION_ROLE_PERMISSION_RESOLVER = ConfigurationRolePermissionResolver.class;

    private final Configuration configuration;
    private final Map<Class<?>, Collection<Class<?>>> securityClasses;
    private final Collection<Class<? extends PrincipalCustomizer<?>>> principalCustomizerClasses;
    private Collection<RealmConfiguration> configurationRealms;

    SecurityConfigurer(Configuration configuration, Map<Class<?>, Collection<Class<?>>> securityClasses,
                       Collection<Class<? extends PrincipalCustomizer<?>>> principalCustomizerClasses) {
        this.configuration = configuration;
        this.securityClasses = securityClasses;
        this.principalCustomizerClasses = principalCustomizerClasses;
        if (CollectionUtils.isEmpty(securityClasses.get(Realm.class))) {
            throw new IllegalArgumentException("No realm class provided !");
        }
    }

    Collection<Class<? extends PrincipalCustomizer<?>>> getPrincipalCustomizers() {
        if (principalCustomizerClasses != null) {
            return principalCustomizerClasses;
        }
        return Collections.emptyList();
    }

    Collection<RealmConfiguration> getConfigurationRealms() {
        if (configurationRealms == null) {
            buildRealms();
        }
        return Collections.unmodifiableCollection(configurationRealms);
    }

    Configuration getSecurityConfiguration() {
        return this.configuration;
    }

    @SuppressWarnings("unchecked")
    private void buildRealms() {
        configurationRealms = new ArrayList<RealmConfiguration>();
        String[] realmNames = configuration.getStringArray(REALMS_KEY);
        if (ArrayUtils.isEmpty(realmNames)) {
            RealmConfiguration confRealm = new RealmConfiguration(DEFAULT_REALM.getSimpleName(), DEFAULT_REALM);
            configurationRealms.add(confRealm);
        } else {
            for (String realmName : realmNames) {
                Class<? extends Realm> realmClass = (Class<? extends Realm>) findClass(realmName, securityClasses.get(Realm.class));
                if (realmClass == null) {
                    throw new IllegalArgumentException("Unknown realm defined in property " + REALMS_KEY + " : " + realmName);
                }
                RealmConfiguration confRealm = new RealmConfiguration(realmName, realmClass);
                configurationRealms.add(confRealm);
            }
        }
        for (RealmConfiguration confRealm : configurationRealms) {
            confRealm.setRolePermissionResolverClass(findRolePermissionResolver(confRealm));
            confRealm.setRoleMappingClass(findRoleMapping(confRealm));
        }
    }

    private Class<? extends RolePermissionResolver> findRolePermissionResolver(RealmConfiguration realm) {
        String rolePermissionResolver = configuration.getString(realm.getName() + ROLE_PERMISSION_RESOLVER_KEY);
        if (rolePermissionResolver == null) {
            if (configuration.subset(ConfigurationRolePermissionResolver.PERMISSIONS_SECTION_NAME).isEmpty()) {
                return DEFAULT_ROLE_PERMISSION_RESOLVER;
            }
            return CONFIGURATION_ROLE_PERMISSION_RESOLVER;
        }
        return findRealmComponent(realm.getName(), rolePermissionResolver, RolePermissionResolver.class);
    }

    private Class<? extends RoleMapping> findRoleMapping(RealmConfiguration realm) {
        String roleMapping = configuration.getString(realm.getName() + ROLE_MAPPING_KEY);
        if (roleMapping == null) {
            if (configuration.subset(ConfigurationRoleMapping.ROLES_SECTION_NAME).isEmpty()) {
                return DEFAULT_ROLE_MAPPING;
            }
            return CONFIGURATION_ROLE_MAPPING;
        }
        return findRealmComponent(realm.getName(), roleMapping, RoleMapping.class);
    }

    @SuppressWarnings("unchecked")
    private <T> Class<? extends T> findRealmComponent(String realmName, String componentName, Class<? extends T> clazz) {
        Class<? extends T> componentClass;
        if (CollectionUtils.isEmpty(securityClasses.get(clazz))) {
            throw new IllegalArgumentException("No class of type " + componentName + " were found");
        }
        componentClass = (Class<? extends T>) findClass(componentName, securityClasses.get(clazz));
        if (componentClass == null) {
            throw new IllegalArgumentException("Unknown property value " + componentName + " for realm " + realmName);
        }
        return componentClass;
    }

    private Class<?> findClass(String name, Collection<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            if (clazz.getSimpleName().equals(name)) {
                return clazz;
            }
        }
        return null;
    }
}
