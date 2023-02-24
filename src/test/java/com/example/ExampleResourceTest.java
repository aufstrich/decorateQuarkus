package com.example;

import io.quarkus.test.junit.QuarkusTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
class ExampleResourceTest {

    @Inject
    DoesStuff blup;


    @Test
    void testHelloEndpoint() {
        String result = blup.doStuff();
        Assertions.assertThat(result).isEqualTo("base->transactional->retrying->metered");
    }

}
