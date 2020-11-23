export SPRING_DATASOURCE_URL=$(cat /secrets/syfomotebehovdb/config/jdbc_url)
export SPRING_DATASOURCE_USERNAME=$(cat /secrets/syfomotebehovdb/credentials/username)
export SPRING_DATASOURCE_PASSWORD=$(cat /secrets/syfomotebehovdb/credentials/password)
