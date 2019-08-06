import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.models.{V1ConfigMapBuilder, V1Deployment, V1DeploymentBuilder, V1PersistentVolumeClaimBuilder, V1Service, V1ServiceBuilder}
import collection.JavaConverters._

object CromwellComponents {

  val service: V1Service =
    new V1ServiceBuilder().
      withNewMetadata().withName("cromwell-service").endMetadata().
      withNewSpec().addNewPort().withPort(8000).endPort().addToSelector("app", "cromwell").endSpec().
      build()

  val worker: V1Deployment =
    new V1DeploymentBuilder().
      withNewMetadata().withNewName("cromwell").endMetadata().
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
