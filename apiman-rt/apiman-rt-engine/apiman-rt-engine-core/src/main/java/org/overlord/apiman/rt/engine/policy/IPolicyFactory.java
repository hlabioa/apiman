/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.overlord.apiman.rt.engine.policy;

import org.overlord.apiman.rt.engine.beans.exceptions.PolicyNotFoundException;

/**
 * Factory used to create instances of policies.  Policies must be stateless
 * and thread safe.
 *
 * @author eric.wittmann@redhat.com
 */
public interface IPolicyFactory {
    
    /**
     * Gets a policy using the policy-impl information provided.
     * @param policyImpl
     * @throws PolicyNotFoundException
     */
    public IPolicy getPolicy(String policyImpl) throws PolicyNotFoundException;

}
