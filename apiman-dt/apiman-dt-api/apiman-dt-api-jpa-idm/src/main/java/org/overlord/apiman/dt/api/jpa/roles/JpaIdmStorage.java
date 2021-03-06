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
package org.overlord.apiman.dt.api.jpa.roles;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.overlord.apiman.dt.api.beans.idm.PermissionBean;
import org.overlord.apiman.dt.api.beans.idm.RoleBean;
import org.overlord.apiman.dt.api.beans.idm.RoleMembershipBean;
import org.overlord.apiman.dt.api.beans.idm.UserBean;
import org.overlord.apiman.dt.api.beans.search.SearchCriteriaBean;
import org.overlord.apiman.dt.api.beans.search.SearchResultsBean;
import org.overlord.apiman.dt.api.core.IIdmStorage;
import org.overlord.apiman.dt.api.core.exceptions.AlreadyExistsException;
import org.overlord.apiman.dt.api.core.exceptions.DoesNotExistException;
import org.overlord.apiman.dt.api.core.exceptions.StorageException;
import org.overlord.apiman.dt.api.jpa.AbstractJpaStorage;
import org.overlord.apiman.dt.api.jpa.JpaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JPA implementation of the role storage interface {@link IIdmStorage}.
 *
 * @author eric.wittmann@redhat.com
 */
@ApplicationScoped
public class JpaIdmStorage extends AbstractJpaStorage implements IIdmStorage {
    
    private static Logger logger = LoggerFactory.getLogger(JpaIdmStorage.class);

    /**
     * Constructor.
     */
    public JpaIdmStorage() {
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#createUser(org.overlord.apiman.dt.api.beans.idm.UserBean)
     */
    @Override
    public void createUser(UserBean user) throws StorageException, AlreadyExistsException {
        user.setJoinedOn(new Date());
        super.create(user);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#getUser(java.lang.String)
     */
    @Override
    public UserBean getUser(String userId) throws StorageException, DoesNotExistException {
        return super.get(userId, UserBean.class);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#updateUser(org.overlord.apiman.dt.api.beans.idm.UserBean)
     */
    @Override
    public void updateUser(UserBean user) throws StorageException, DoesNotExistException {
        super.update(user);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#findUsers(org.overlord.apiman.dt.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<UserBean> findUsers(SearchCriteriaBean criteria) throws StorageException {
        return super.find(criteria, UserBean.class);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#createRole(org.overlord.apiman.dt.api.beans.idm.RoleBean)
     */
    @Override
    public void createRole(RoleBean role) throws StorageException, AlreadyExistsException {
        super.create(role);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#updateRole(org.overlord.apiman.dt.api.beans.idm.RoleBean)
     */
    @Override
    public void updateRole(RoleBean role) throws StorageException, DoesNotExistException {
        super.update(role);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#deleteRole(org.overlord.apiman.dt.api.beans.idm.RoleBean)
     */
    @Override
    public void deleteRole(RoleBean role) throws StorageException, DoesNotExistException {
        // First delete all memberships in this role
        EntityManager entityManager = getEmfAccessor().getEntityManagerFactory().createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Query query = entityManager.createQuery("DELETE from RoleMembershipBean m WHERE m.roleId = :roleId" ); //$NON-NLS-1$
            query.setParameter("roleId", role.getId()); //$NON-NLS-1$
            query.executeUpdate();
            entityManager.getTransaction().commit();
        } catch (Throwable t) {
            JpaUtil.rollbackQuietly(entityManager);
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            entityManager.close();
        }
        // Then delete the role itself.
        super.delete(role);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#getRole(java.lang.String)
     */
    @Override
    public RoleBean getRole(String roleId) throws StorageException, DoesNotExistException {
        return super.get(roleId, RoleBean.class);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#findRoles(org.overlord.apiman.dt.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<RoleBean> findRoles(SearchCriteriaBean criteria) throws StorageException {
        return super.find(criteria, RoleBean.class);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#createMembership(org.overlord.apiman.dt.api.beans.idm.RoleMembershipBean)
     */
    @Override
    public void createMembership(RoleMembershipBean membership) throws StorageException,
            AlreadyExistsException {
        super.create(membership);
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#deleteMembership(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void deleteMembership(String userId, String roleId, String organizationId)
            throws StorageException, DoesNotExistException {
        EntityManager entityManager = getEmfAccessor().getEntityManagerFactory().createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Query query = entityManager.createQuery("DELETE FROM RoleMembershipBean m WHERE m.roleId = :roleId AND m.userId = :userId AND m.organizationId = :orgId" ); //$NON-NLS-1$
            query.setParameter("roleId", roleId); //$NON-NLS-1$
            query.setParameter("userId", userId); //$NON-NLS-1$
            query.setParameter("orgId", organizationId); //$NON-NLS-1$
            if (query.executeUpdate() == 0)
                throw new DoesNotExistException();
            entityManager.getTransaction().commit();
        } catch (DoesNotExistException dne) {
            JpaUtil.rollbackQuietly(entityManager);
            throw dne;
        } catch (Throwable t) {
            JpaUtil.rollbackQuietly(entityManager);
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            entityManager.close();
        }
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#deleteMemberships(java.lang.String, java.lang.String)
     */
    @Override
    public void deleteMemberships(String userId, String organizationId) throws StorageException {
        EntityManager entityManager = getEmfAccessor().getEntityManagerFactory().createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Query query = entityManager.createQuery("DELETE FROM RoleMembershipBean m WHERE m.userId = :userId AND m.organizationId = :orgId" ); //$NON-NLS-1$
            query.setParameter("userId", userId); //$NON-NLS-1$
            query.setParameter("orgId", organizationId); //$NON-NLS-1$
            query.executeUpdate();
            entityManager.getTransaction().commit();
        } catch (Throwable t) {
            JpaUtil.rollbackQuietly(entityManager);
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            entityManager.close();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#getUserMemberships(java.lang.String)
     */
    @Override
    public Set<RoleMembershipBean> getUserMemberships(String userId) throws StorageException {
        Set<RoleMembershipBean> memberships = new HashSet<RoleMembershipBean>();
        EntityManager entityManager = getEmfAccessor().getEntityManagerFactory().createEntityManager();
        try {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<RoleMembershipBean> criteriaQuery = builder.createQuery(RoleMembershipBean.class);
            Root<RoleMembershipBean> from = criteriaQuery.from(RoleMembershipBean.class);
            criteriaQuery.where(builder.equal(from.get("userId"), userId)); //$NON-NLS-1$
            TypedQuery<RoleMembershipBean> typedQuery = entityManager.createQuery(criteriaQuery);
            List<RoleMembershipBean> resultList = typedQuery.getResultList();
            memberships.addAll(resultList);
            return memberships;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            entityManager.close();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#getUserMemberships(java.lang.String, java.lang.String)
     */
    @Override
    public Set<RoleMembershipBean> getUserMemberships(String userId, String organizationId) throws StorageException {
        Set<RoleMembershipBean> memberships = new HashSet<RoleMembershipBean>();
        EntityManager entityManager = getEmfAccessor().getEntityManagerFactory().createEntityManager();
        try {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<RoleMembershipBean> criteriaQuery = builder.createQuery(RoleMembershipBean.class);
            Root<RoleMembershipBean> from = criteriaQuery.from(RoleMembershipBean.class);
            criteriaQuery.where(
                    builder.equal(from.get("userId"), userId), //$NON-NLS-1$
                    builder.equal(from.get("organizationId"), organizationId) ); //$NON-NLS-1$
            TypedQuery<RoleMembershipBean> typedQuery = entityManager.createQuery(criteriaQuery);
            List<RoleMembershipBean> resultList = typedQuery.getResultList();
            memberships.addAll(resultList);
            return memberships;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            entityManager.close();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#getOrgMemberships(java.lang.String)
     */
    @Override
    public Set<RoleMembershipBean> getOrgMemberships(String organizationId) throws StorageException {
        Set<RoleMembershipBean> memberships = new HashSet<RoleMembershipBean>();
        EntityManager entityManager = getEmfAccessor().getEntityManagerFactory().createEntityManager();
        try {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<RoleMembershipBean> criteriaQuery = builder.createQuery(RoleMembershipBean.class);
            Root<RoleMembershipBean> from = criteriaQuery.from(RoleMembershipBean.class);
            criteriaQuery.where(builder.equal(from.get("organizationId"), organizationId)); //$NON-NLS-1$
            TypedQuery<RoleMembershipBean> typedQuery = entityManager.createQuery(criteriaQuery);
            List<RoleMembershipBean> resultList = typedQuery.getResultList();
            memberships.addAll(resultList);
            return memberships;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            entityManager.close();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IIdmStorage#getPermissions(java.lang.String)
     */
    @Override
    public Set<PermissionBean> getPermissions(String userId) throws StorageException {
        Set<PermissionBean> permissions = new HashSet<PermissionBean>();
        EntityManager entityManager = getEmfAccessor().getEntityManagerFactory().createEntityManager();
        try {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<RoleMembershipBean> criteriaQuery = builder.createQuery(RoleMembershipBean.class);
            Root<RoleMembershipBean> from = criteriaQuery.from(RoleMembershipBean.class);
            criteriaQuery.where(builder.equal(from.get("userId"), userId)); //$NON-NLS-1$
            TypedQuery<RoleMembershipBean> typedQuery = entityManager.createQuery(criteriaQuery);
            List<RoleMembershipBean> resultList = typedQuery.getResultList();
            for (RoleMembershipBean membership : resultList) {
                RoleBean role = getRole(membership.getRoleId());
                String qualifier = membership.getOrganizationId();
                for (String permission : role.getPermissions()) {
                    PermissionBean p = new PermissionBean();
                    p.setName(permission);
                    p.setOrganizationId(qualifier);
                    permissions.add(p);
                }
            }
            return permissions;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            entityManager.close();
        }
    }

}
