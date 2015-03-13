/*
 * Copyright 2011 Red Hat inc. and third party contributors as noted 
 * by the author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ceylon.modules.jboss.runtime;

import java.util.NavigableMap;
import java.util.Set;

import ceylon.modules.CeylonRuntimeException;
import ceylon.modules.Configuration;
import ceylon.modules.Main;
import ceylon.modules.api.runtime.AbstractRuntime;
import ceylon.modules.spi.runtime.ClassLoaderHolder;

import com.redhat.ceylon.common.log.Logger;
import com.redhat.ceylon.cmr.api.ModuleQuery;
import com.redhat.ceylon.cmr.api.ModuleVersionDetails;
import com.redhat.ceylon.cmr.api.ModuleVersionQuery;
import com.redhat.ceylon.cmr.api.ModuleVersionResult;
import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.cmr.api.RepositoryManagerBuilder;
import com.redhat.ceylon.cmr.ceylon.CeylonUtils;
import com.redhat.ceylon.cmr.impl.CMRJULLogger;
import com.redhat.ceylon.cmr.spi.ContentTransformer;
import com.redhat.ceylon.cmr.spi.MergeStrategy;
import com.redhat.ceylon.common.Versions;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleNotFoundException;

/**
 * Abstract Ceylon JBoss Modules runtime.
 * Useful for potential extension.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractJBossRuntime extends AbstractRuntime {
    public ClassLoaderHolder createClassLoader(String name, String version, Configuration conf) throws Exception {
        if (RepositoryManager.DEFAULT_MODULE.equals(name)) {
            if (version != null) {
                throw new CeylonRuntimeException("Invalid module identifier: default module should not have any version");
            }
        } else {
            if (version == null) {
                StringBuilder sb = new StringBuilder("Invalid module identifier: missing required version");
                sb.append(" (should be of the form ");
                sb.append(name);
                sb.append("/version)");
                throw new CeylonRuntimeException(sb.toString());
            }
        }

        ModuleIdentifier moduleIdentifier;
        try {
            moduleIdentifier = ModuleIdentifier.fromString(name + ":" + version);
        } catch (IllegalArgumentException x) {
            CeylonRuntimeException cre = new CeylonRuntimeException("Invalid module name or version: contains invalid characters");
            cre.initCause(x);
            throw cre;
        }
        try {
            ModuleLoader moduleLoader = createModuleLoader(conf);
            Module module = moduleLoader.loadModule(moduleIdentifier);
            return new ClassLoaderHolderImpl(module);
        } catch (ModuleNotFoundException e) {
            String spec = e.getMessage();
            int p = spec.lastIndexOf(':');
            if (p >= 0) {
                spec = spec.substring(0, p) + "/" + spec.substring(p + 1);
            }
            final CeylonRuntimeException cre = new CeylonRuntimeException("Could not find module: " + spec + " (invalid version?)");
            cre.initCause(e);
            throw cre;
        }
    }

    private Set<String> getVersions(String name, Configuration conf, boolean offline) {
        RepositoryManager repoman = createRepository(conf, offline);
        ModuleVersionQuery query = new ModuleVersionQuery(name, null, ModuleQuery.Type.JVM);
        query.setBinaryMajor(Versions.JVM_BINARY_MAJOR_VERSION);
        ModuleVersionResult result = repoman.completeVersions(query);
        NavigableMap<String, ModuleVersionDetails> versionMap = result.getVersions();
        return versionMap.keySet();
    }

    private void appendVersions(StringBuilder sb, String name, Set<String> versions) {
        for (String v : versions) {
            sb.append("\n    ");
            sb.append(name);
            sb.append("/");
            sb.append(v);
        }
    }

    private RepositoryManager createRepository(Configuration conf, boolean offline) {
        Logger log = new CMRJULLogger();
        final RepositoryManagerBuilder builder = CeylonUtils.repoManager()
            .cwd(conf.cwd)
            .systemRepo(conf.systemRepository)
            .cacheRepo(conf.cacheRepository)
            .overrides(conf.overrides)
            .noDefaultRepos(conf.noDefaultRepositories)
            .userRepos(conf.repositories)
            .offline(offline || conf.offline)
            .logger(log)
            .buildManagerBuilder();

        final MergeStrategy ms = getService(MergeStrategy.class, conf);
        if (ms != null)
            builder.mergeStrategy(ms);

        if (conf.cacheContent)
            builder.cacheContent();

        final ContentTransformer ct = getService(ContentTransformer.class, conf);
        if (ct != null)
            builder.contentTransformer(ct);

        return builder.buildRepository();
    }


    /**
     * Get repository extension.
     *
     * @param conf the configuration
     * @return repository extension
     */
    protected RepositoryManager createRepository(Configuration conf) {
        return createRepository(conf, false);
    }

    /**
     * Get repository service.
     *
     * @param serviceType the service type
     * @param conf        the configuration
     * @return service instance or null
     */
    protected <T> T getService(Class<T> serviceType, Configuration conf) {
        try {
            String impl = conf.impl.get(serviceType.getName());
            return (impl != null) ? Main.createInstance(serviceType, impl) : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate service: " + serviceType.getName(), e);
        }
    }

    /**
     * Create module loader.
     *
     * @param conf the configuration
     * @return the module loader
     * @throws Exception for any error during creation
     */
    protected abstract ModuleLoader createModuleLoader(Configuration conf) throws Exception;
}
