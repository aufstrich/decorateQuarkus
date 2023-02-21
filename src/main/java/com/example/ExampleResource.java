package com.example;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import java.time.Duration;
import java.time.Instant;


class ServiceProducers {

    @Produces
    BaseDoStuff produceBaseStuff() {
        return new BaseDoStuff();
    }

    @Produces
    TransactionalDoStuff produceTransactionalDoStuff(
            BaseDoStuff baseDoStuff
    ) {
        return new TransactionalDoStuff(baseDoStuff);
    }

    @Produces
    RetryingDoeStuff produceRetryingDoeStuff(
            TransactionalDoStuff transactionalDoStuff
    ) {
        return new RetryingDoeStuff(transactionalDoStuff);
    }

    @Produces
    MeteredDoeStuff produceRetryingDoeStuff(
            RetryingDoeStuff retryingDoeStuff
    ) {
        return new MeteredDoeStuff(retryingDoeStuff);
    }

    @Produces
    FinalConsumer produceFinalConsumer(MeteredDoeStuff meteredDoeStuff) {
        return new FinalConsumer(meteredDoeStuff);
    }


    void onStart(@Observes StartupEvent ev, FinalConsumer finalConsumer) {
        finalConsumer.okCool();
    }

}

interface DoesStuff {
    String doStuff();
}

class BaseDoStuff implements DoesStuff {

    @Override
    public String doStuff() {
        return "base";
    }
}

@RequiredArgsConstructor
class TransactionalDoStuff implements DoesStuff {
    private final DoesStuff delegate;

    @Override
    public String doStuff() {
        return QuarkusTransaction.requiringNew().call(() -> "transactional" + this.delegate.doStuff());
    }
}

@RequiredArgsConstructor
class RetryingDoeStuff implements DoesStuff {

    private final DoesStuff delegate;

    @Override
    public String doStuff() {
        return "retrying" + this.delegate.doStuff();
    }
}

@JBossLog
@RequiredArgsConstructor
class MeteredDoeStuff implements DoesStuff {

    private final DoesStuff delegate;

    @Override
    @SneakyThrows
    public String doStuff() {
        Instant start = Instant.now();
        Instant end = null;
        try {
            String result = "metered" + this.delegate.doStuff();
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

@JBossLog
@RequiredArgsConstructor
class FinalConsumer {
    private final DoesStuff delegate;

    public void okCool() {
        log.infof("ja passt: %s", delegate.doStuff());
    }

}



