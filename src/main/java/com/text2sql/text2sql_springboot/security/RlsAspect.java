package com.text2sql.text2sql_springboot.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RlsAspect {
    @PersistenceContext
    private EntityManager em;

    @Before("@within(jakarta.transaction.Transactional) || " +
            "@annotation(jakarta.transaction.Transactional)")
    public void setRlsUser() {
        String uid = AppContext.getCurrentUserId();
        if (uid != null) {
            em.createNativeQuery("select set_config('app.current_user_id', :uid, true)")
                    .setParameter("uid", uid)
                    .getSingleResult();
        }
    }
}
