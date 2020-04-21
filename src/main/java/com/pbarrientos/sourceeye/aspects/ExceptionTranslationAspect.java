package com.pbarrientos.sourceeye.aspects;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jboss.logging.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;

/**
 * Class that wraps the calls to the services and translate the possible thrown exceptions to
 * {@link SourceEyeServiceException}
 *
 * @author Pablo Barrientos
 */
@Order(1)
@Aspect
@Component
public class ExceptionTranslationAspect {

    private final Logger log = Logger.getLogger(this.getClass());

    @Around("execution(* com.pbarrientos.sourceeye.data.services..*(..))")
    public Object translateServiceExceptions(final ProceedingJoinPoint pjp) throws Throwable {
        try {
            this.log.debug("Se va a realizar la operacion: " + pjp.getSignature());
            return pjp.proceed();
        } catch (Throwable e) {
            Integer nArgs = pjp.getArgs().length;
            String args = "";
            if (nArgs > 0) {
                args += pjp.getArgs()[0];
                for (int i = 1; i < nArgs; i++) {
                    args += ", " + pjp.getArgs()[i];
                }
            }
            String message = String.format("[ERROR - %s] %s#%s(%s) - %s ",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    pjp.getSignature().getDeclaringType(), pjp.getSignature().getName(), args, e.getLocalizedMessage());
            this.log.error(message);
            throw new SourceEyeServiceException(e);
        }
    }

}
