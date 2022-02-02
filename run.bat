cls
cd %cd%
c:
javac -cp javax.mail.jar;jsch-0.1.55.jar;ojdbc6.jar Monitor.java
java -cp .;javax.mail.jar;jsch-0.1.55.jar;ojdbc6.jar Monitor > log.txt
rem java -cp .;javax.mail.jar;jsch-0.1.55.jar;ojdbc6.jar Monitor 
