# Generated automatically from Makefile.in by configure.
# Generated automatically from Makefile.in by configure.
.SUFFIXES: .java .class

BUILD_DIR=..

JAVAC=/usr/local/src/java/javac_generic/scripts/javac -warnunchecked
JFLAGS=
RESIN=/home/ferg/ws/resin

CLASS_DIR=$(BUILD_DIR)/classes
D=$(CLASS_DIR)
CLASSPATH=.:$(CLASS_DIR):$(JAVA_HOME)/jre/lib/rt.jar:$(RESIN)/classes:$(RESIN)/lib/dom.jar:$(RESIN)/lib/sax.jar:$(RESIN)/lib/jaxp.jar:$(RESIN)/lib/jsdk-24.jar:$(RESIN)/lib/ejb.jar:$(RESIN)/lib/jta-101.jar:$(RESIN)/lib/jms-11.jar:$(RESIN)/lib/jca-15.jar:$(RESIN)/lib/jmx-12.jar:$(RESIN)/lib/isorelax.jar:$(RESIN)/lib/hibernate-api.jar
XML_SRC=$(shell find * -name CVS -prune -o -name \*.xml -o -name \*.tld -o -name \*.rnc -print)
XML_OBJ=$(patsubst %,$D/%,$(XML_SRC))

SRC=$(shell find * -name CVS -prune -o -name \*.java -print)
OBJ=$(patsubst %.java,$(CLASS_DIR)/%.class,$(SRC))

all	: $(OBJ) $(XML_OBJ)

$D/%	: %
	cd ../src; cp $< $D/$<

$(OBJ)	: $(CLASS_DIR)

$(CLASS_DIR)	:
	- mkdir -p $(CLASS_DIR)

$(CLASS_DIR)/%.class	: %.java
	$(JAVAC) $(JFLAGS) -classpath $(CLASSPATH) -d $(CLASS_DIR) $<
