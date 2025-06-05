FROM kalilinux/kali-rolling

# Install amass and other dependencies
RUN apt-get update && \
    apt-get install -y amass webhook curl wget unzip && \
    mkdir -p /data/amass-results

WORKDIR /data

# Copy your hooks.json (make sure it's in the build context)
COPY hooks.json /data/hooks.json

EXPOSE 9000

CMD ["webhook", "-hooks", "/data/hooks.json", "-port", "9000", "-verbose"]
