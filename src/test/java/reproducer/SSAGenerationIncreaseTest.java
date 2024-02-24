package reproducer;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class SSAGenerationIncreaseTest {

    public static final String ANNOTATION_KEY = "io.test";
//    ok found:
//    https://midbai.com/en/post/meta-generation-increasing-strategy-exploration/#generationchangedpredicate-filter-status-principle

    @Test
    void generationIncreasesWithJustAddingAnnotation() {
        var c = new KubernetesClientBuilder().build();
        var dep = new KubernetesSerialization().unmarshal(deployment, Deployment.class);

        var created = c.resource(dep).forceConflicts().serverSideApply();
        var updatedWithoutAnnotation = c.resource(dep).forceConflicts().serverSideApply();

        assertThat(created.getMetadata().getGeneration()).isEqualTo(updatedWithoutAnnotation.getMetadata().getGeneration());


        dep.getMetadata().getAnnotations().put(ANNOTATION_KEY,""+System.currentTimeMillis());
        var updatedAnnotation = c.resource(dep).forceConflicts().serverSideApply();

        // this fails
        assertThat(updatedAnnotation.getMetadata().getGeneration()).isEqualTo(updatedWithoutAnnotation.getMetadata().getGeneration());
    }

    @Test
    void ifNoSSAGenerationDoesNotIncrease() {
        var c = new KubernetesClientBuilder().build();
        var def = new KubernetesSerialization().unmarshal(deployment, Deployment.class);
        def.getMetadata().setName("test"+System.currentTimeMillis());

        var created = c.resource(def).create();

        created.getMetadata().getAnnotations().put(ANNOTATION_KEY,""+System.currentTimeMillis());
        created.getMetadata().setResourceVersion(null);
        var updatedAnnotation = c.resource(created).update();

        // this fails
        assertThat(created.getMetadata().getGeneration()).isEqualTo(updatedAnnotation.getMetadata().getGeneration());
    }

    void testConfigMap() {

    }

    String deployment = """
                    apiVersion: apps/v1 #
                    kind: Deployment
                    metadata:
                      name: testx
                    spec:
                      selector:
                        matchLabels:
                          app: testx
                      replicas: 1
                      template:
                        metadata:
                          labels:
                            app: testx
                        spec:
                          containers:
                            - name: nginx
                              image: nginx:1.17.0
                              ports:
                                - containerPort: 80
                        
            """;
}
