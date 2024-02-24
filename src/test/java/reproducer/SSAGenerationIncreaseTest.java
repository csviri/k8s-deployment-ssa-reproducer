package reproducer;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SSAGenerationIncreaseTest {

    public static final String ANNOTATION_KEY = "io.test";

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
