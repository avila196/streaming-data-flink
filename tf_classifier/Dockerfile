FROM python:3.7

WORKDIR /docker

COPY . /docker

RUN pip install -r requirements.txt

WORKDIR /docker/app

EXPOSE 5000

CMD ["python", "classifier_server.py"]