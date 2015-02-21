-- MySQL dump 10.13  Distrib 5.6.23, for osx10.8 (x86_64)
--
-- Host: localhost    Database: myIM
-- ------------------------------------------------------
-- Server version	5.6.23

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `lu`
--

DROP TABLE IF EXISTS `lu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `lu` (
  `friendnm` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lu`
--

LOCK TABLES `lu` WRITE;
/*!40000 ALTER TABLE `lu` DISABLE KEYS */;
INSERT INTO `lu` VALUES ('yu');
/*!40000 ALTER TABLE `lu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `userid`
--

DROP TABLE IF EXISTS `userid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userid` (
  `usernm` varchar(25) NOT NULL DEFAULT '',
  `password` varchar(255) NOT NULL,
  `isOnline` tinyint(1) NOT NULL,
  `IP_port` varchar(20) NOT NULL,
  `IP` varchar(15) DEFAULT NULL,
  `Port` int(10) DEFAULT NULL,
  PRIMARY KEY (`usernm`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `userid`
--

LOCK TABLES `userid` WRITE;
/*!40000 ALTER TABLE `userid` DISABLE KEYS */;
INSERT INTO `userid` VALUES ('lu','1',0,'0.0.0.0:0000','0.0.0.0',0),('yu','1',0,'0.0.0.0:0000','0.0.0.0',0);
/*!40000 ALTER TABLE `userid` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `yu`
--

DROP TABLE IF EXISTS `yu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `yu` (
  `friendnm` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `yu`
--

LOCK TABLES `yu` WRITE;
/*!40000 ALTER TABLE `yu` DISABLE KEYS */;
INSERT INTO `yu` VALUES ('lu');
/*!40000 ALTER TABLE `yu` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-02-22  0:35:14
