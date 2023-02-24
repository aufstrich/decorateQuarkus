package com.example;

import io.quarkus.arc.Priority;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;


class ServiceProducers {

    void onStart(@Observes StartupEvent ev, FinalConsumer finalConsumer) {
        finalConsumer.okCool();
    }

}

interface DoesStuff {
    String doStuff();
}

@ApplicationScoped
class BaseDoStuff implements DoesStuff {

    @Override
    public String doStuff() {
        return "base";
    }
}

@RequiredArgsConstructor
@Decorator
@Priority(30)
class TransactionalDoStuff implements DoesStuff {
    @Inject
    @Any
    @Delegate
    DoesStuff delegate;

    @Override
    public String doStuff() {
        return QuarkusTransaction.requiringNew().call(() -> this.delegate.doStuff() + "->transactional");
    }
}

@Decorator
@Priority(20)
class RetryingDoeStuff implements DoesStuff {
    @Inject
    @Any
    @Delegate
    DoesStuff delegate;

    @Override
    public String doStuff() {
        return this.delegate.doStuff() + "->retrying";
    }
}

@JBossLog
@Decorator
@Priority(10)
class MeteredDoeStuff implements DoesStuff {

    @Inject
    @Any
    @Delegate
    DoesStuff delegate;

    @Override
    @SneakyThrows
    public String doStuff() {
        Instant start = Instant.now();
        Instant end = null;
        try {
            String result = this.delegate.doStuff() + "->metered";
            end = Instant.now();
            return result;
        } catch (Exception e) {
            end = Instant.now();
            throw e;
        } finally {
            log.infof("execution took %s ms", Duration.between(start, end).toMillis());
        }
    }
}

@ApplicationScoped
@JBossLog
@RequiredArgsConstructor
class FinalConsumer {
    private final DoesStuff delegate;

    public void okCool() {
        log.infof("ja passt: %s", delegate.doStuff());
    }

}



