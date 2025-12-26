FROM bellsoft/liberica-openjdk-alpine:21

# 2. Argument: 빌드된 jar 파일의 경로 변수 설정
ARG JAR_FILE=build/libs/*.jar

# 3. Copy: jar 파일을 컨테이너 내부로 복사
COPY ${JAR_FILE} app.jar

# 4. Run: 컨테이너가 실행될 때 자바 프로그램 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]