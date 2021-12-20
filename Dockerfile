FROM adoptopenjdk/openjdk11:ubi-minimal-jre

RUN mkdir -p /para/lib

WORKDIR /para

ENV PARA_PLUGIN_ID="para-dao-mongodb" \
	PARA_PLUGIN_VER="1.37.0"

ADD https://oss.sonatype.org/service/local/repositories/releases/content/com/erudika/$PARA_PLUGIN_ID/$PARA_PLUGIN_VER/$PARA_PLUGIN_ID-$PARA_PLUGIN_VER-shaded.jar /para/lib/
