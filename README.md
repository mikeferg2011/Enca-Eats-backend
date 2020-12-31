# Enca-Eats


1. Create app subscription -- This will be what manages all 
of our deploys
 
```az appservice plan create -g enca-eats -n enca-eats-app-service-plan --is-linux --sku F1```

2. Create web app -- Each service will need to have a webapp

```az webapp create -g enca-eats -p enca-eats-app-service-plan -n enca-eats-backend --runtime "TOMCAT|8.5-jre8"```

3. Deploy -- This specific repo

```mvn package azure-webapp:deploy```