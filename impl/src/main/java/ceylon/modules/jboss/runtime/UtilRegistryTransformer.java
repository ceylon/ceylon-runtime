/*
 * Copyright 2013 Red Hat inc. and third party contributors as noted
 * by the author tags.
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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import com.redhat.ceylon.cmr.api.ArtifactResult;
import com.redhat.ceylon.cmr.api.RepositoryManager;
import org.jboss.modules.ModuleIdentifier;

/**
 * Per module Util registry.
 * <p/>
 * A bit of a hack / workaround to register stuff on first class load.
 * <p/>
 * Check this again when re-linking usage is more common!
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class UtilRegistryTransformer implements ClassFileTransformer {
    private volatile boolean done;

    private final ModuleIdentifier mi;
    private final ArtifactResult result;

    public UtilRegistryTransformer(ModuleIdentifier mi, ArtifactResult result) {
        this.mi = mi;
        this.result = result;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        register(loader);
        return classfileBuffer;
    }

    static void registerModule(final String name, String _version, final ArtifactResult result, final ClassLoader cl, boolean async) {
        // transform "null" into null version for the default module
        final String version = RepositoryManager.DEFAULT_MODULE.equals(name) ? null : _version;
        // FIXME: this hack is required to make sure we never try to load modules in the metamodel with the
        // classloader lock, because this causes deadlocks: https://github.com/ceylon/ceylon.language/issues/429
        if(async){
            if(cl instanceof CeylonModuleClassLoader){
                ((CeylonModuleClassLoader) cl).registerThreadRunning();
            }
            new Thread(){
                @Override
                public void run() {
                    com.redhat.ceylon.compiler.java.Util.loadModule(name, version, result, cl);
                    if(cl instanceof CeylonModuleClassLoader){
                        ((CeylonModuleClassLoader) cl).registerThreadDone();
                    }
                }
            }.start();
        }else
            com.redhat.ceylon.compiler.java.Util.loadModule(name, version, result, cl);
    }

    public void register(ClassLoader loader) {
        if (done == false) {
            synchronized (this) {
                if (done == false) {
                    done = true;
                    registerModule(mi.getName(), mi.getSlot(), result, loader, true);
                }
            }
        }
    }
}
