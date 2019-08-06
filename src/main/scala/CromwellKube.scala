
import cats.effect.{ExitCode, IO, IOApp}
import io.kubernetes.client.{ApiCallback, ApiClient, ApiException, Configuration}
import io.kubernetes.client.apis.{AppsV1Api, CoreV1Api}
import io.kubernetes.client.models.V1Container
import io.kubernetes.client.models.V1ObjectMeta
import io.kubernetes.client.models.V1Pod
import io.kubernetes.client.models._
import io.kubernetes.client.models.V1PodList
import io.kubernetes.client.models.V1PodSpec
import io.kubernetes.client.util.Config
import java.io.IOException
import java.util
import java.util.Collections

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.iam.v1.{Iam, IamScopes}
import com.google.api.services.iam.v1.model.{Binding, CreateServiceAccountRequest, ServiceAccount, SetIamPolicyRequest}
import io.kubernetes.client.custom.Quantity

object CromwellSA extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = IO {
    val sa = new ServiceAccount;
    sa.setDisplayName("Cromwell Runner")
    val csar = new CreateServiceAccountRequest()
    csar.setAccountId("cromwell-service-account")
    csar.setServiceAccount(sa)

    val credential =
      GoogleCredential.getApplicationDefault()
        .createScoped(Collections.singleton(IamScopes.CLOUD_PLATFORM));

    val client =
      new Iam.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance, credential).setApplicationName("Cromwell Service Account creator").build()

    val newSa: Iam#Projects#ServiceAccounts#Create = client.projects().serviceAccounts().create("projects/" + "broad-dsde-cromwell-dev", csar)

    val b = new Binding()
    b.setRole("storage.objectCreator")

    val x = new SetIamPolicyRequest()

    client.projects().serviceAccounts().setIamPolicy()
    val executed = newSa.execute()
    println(s"created sa: ${executed.getEmail}")

    ExitCode.Success
  }
}


object CromwellKube extends IOApp{
  override def run(args: List[String]): IO[ExitCode] = IO {
    val serviceAccountFile = args.headOption
    //create Service account

    //use existing kubernetes cluster
    import collection.JavaConverters._


    val client = Config.defaultClient();
    client.setDebugging(true)
    Configuration.setDefaultApiClient(client);

    val api = new CoreV1Api();
    val apps = new AppsV1Api();

    val envVar = (new V1EnvVar)
    envVar.setName("CROMWELL_ARGS")
    envVar.setValue("server")

    val deployment: V1Deployment =
      new V1DeploymentBuilder().
        withNewMetadata().withNewName("cromwell").
        endMetadata().
        withNewSpec.
        withReplicas(1).
        withNewSelector().addToMatchLabels("app", "cromwell").endSelector().

        withNewTemplate.
        withNewMetadata().addToLabels("app", "cromwell").endMetadata().
        withNewSpec().
        addNewVolume().withName("config-volume").withNewConfigMap.withName("cromwell-conf").endConfigMap().endVolume().
        addNewContainer().
        withImage("broadinstitute/cromwell:44").
        withName("cromwell").
        addNewEnv().withName("CROMWELL_ARGS").withValue("server").endEnv().
        addNewEnv().withName("JAVA_OPTS").withValue("-Dconfig.file=/conf/cromwell.conf").endEnv().
        addNewVolumeMount().withMountPath("/conf").withName("config-volume").endVolumeMount().
        endContainer().
        endSpec().
        endTemplate.
        endSpec.
        build()

    val service = new V1ServiceBuilder().
      withNewMetadata().withName("cromwell-service").endMetadata().
      withNewSpec().addNewPort().withPort(8000).endPort().addToSelector("app", "cromwell").endSpec().
      build()

    val mysqlDeployment: V1Deployment =
      new V1DeploymentBuilder().
        withNewMetadata().withNewName("mysql").
        endMetadata().
        withNewSpec.
        withReplicas(1).
        withNewSelector().addToMatchLabels("app", "mysql").endSelector().

        withNewTemplate.
        withNewMetadata().addToLabels("app", "mysql").endMetadata().
        withNewSpec().
        addNewContainer().
        withImage("mysql:5.5").
        withName("mysql").
        addNewEnv().withName("MYSQL_ROOT_PASSWORD").withValue("cromwell").endEnv().
        addNewEnv().withName("MYSQL_USER").withValue("cromwell").endEnv().
        addNewEnv().withName("MYSQL_PASSWORD").withValue("cromwell").endEnv().
        addNewEnv().withName("MYSQL_DATABASE").withValue("cromwell").endEnv().
        addNewPort().withContainerPort(3306).endPort().
        addNewVolumeMount().withMountPath("/var/lib/mysql").withName("mysql-persistent-storage").endVolumeMount().
        endContainer().
        addNewVolume().withName("mysql-persistent-storage").withNewPersistentVolumeClaim().withClaimName("mysql-pv-claim2").endPersistentVolumeClaim().endVolume().
        endSpec().
        endTemplate.
        endSpec.
        build()

    val mysqlService = new V1ServiceBuilder().
      withNewMetadata().withName("mysql-service").endMetadata().
      withNewSpec().addNewPort().withPort(3306).endPort().addToSelector("app", "mysql").endSpec().
      build()

    val pvc = new V1PersistentVolumeClaimBuilder().withNewMetadata().withName("mysql-pv-claim2").endMetadata().
      withNewSpec().withAccessModes("ReadWriteOnce").withNewResources().addToRequests("storage", new Quantity("20Gi")).endResources().endSpec().build()

    val cfg = new V1ConfigMapBuilder().
      withNewMetadata().withName("cromwell-conf").endMetadata().
      withData(Map("cromwell.conf" -> conf).asJava).build()

//    api.createNamespacedConfigMap("default", cfg, null, null, null)
    apps.createNamespacedDeployment("default", deployment, null, null, null)
    //    api.createNamespacedService("default", service, null, null, null)
//    api.createNamespacedPersistentVolumeClaim("default", pvc, null, null, null)
//    apps.createNamespacedDeployment("default", mysqlDeployment, null, null, null)
//        api.createNamespacedService("default", mysqlService, null, null, null)

    ExitCode.Success
  }

  def conf =
    """
      |database {
      |  profile = "slick.jdbc.MySQLProfile$"
      |  db {
      |    driver = "com.mysql.cj.jdbc.Driver"
      |    url = "jdbc:mysql://mysql/cromwell?rewriteBatchedStatements=true&useSSL=false"
      |    user = "cromwell"
      |    password = "cromwell"
      |    connectionTimeout = 5000
      |  }
      |}
      |
    """.stripMargin
}
