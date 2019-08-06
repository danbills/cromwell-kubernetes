for i in storage.objectCreator storage.objectViewer genomics.pipelinesRunner genomics.admin iam.serviceAccountUser
do
    gcloud projects add-iam-policy-binding broad-dsde-cromwell-dev --member serviceAccount:db-helm-cli@broad-dsde-cromwell-dev.iam.gserviceaccount.com --role roles/$i
done

gcloud iam service-accounts keys create sa.json --iam-account db-helm-cli@broad-dsde-cromwell-dev.iam.gserviceaccount.com
