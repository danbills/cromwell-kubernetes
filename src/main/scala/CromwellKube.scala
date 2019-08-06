
import cats.effect.{ExitCode, IO, IOApp}
import io.kubernetes.client.Configuration
import io.kubernetes.client.apis.{AppsV1Api, CoreV1Api}
import io.kubernetes.client.models._



object CromwellKube extends IOApp{
  override def run(args: List[String]): IO[ExitCode] = IO {

    val serviceAccountFile: String = args.headOption.getOrElse("/home/dan/.kube/config")



    import io.kubernetes.client.util.ClientBuilder
    import io.kubernetes.client.util.KubeConfig
    import java.io.FileReader
    val client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(serviceAccountFile))).build
    client.setDebugging(true)
    Configuration.setDefaultApiClient(client);

    val api = new CoreV1Api();
    val apps = new AppsV1Api();

    val envVar = (new V1EnvVar)
    envVar.setName("CROMWELL_ARGS")
    envVar.setValue("server")

//    api.createNamespacedConfigMap("default", cfg, null, null, null)
//    apps.createNamespacedDeployment("default", deployment, null, null, null)
    //    api.createNamespacedService("default", service, null, null, null)
//    api.createNamespacedPersistentVolumeClaim("default", pvc, null, null, null)
//    apps.createNamespacedDeployment("default", mysqlDeployment, null, null, null)
//        api.createNamespacedService("default", mysqlService, null, null, null)

    ExitCode.Success
  }

}
