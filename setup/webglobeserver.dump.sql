-- MySQL dump 10.13  Distrib 5.7.16, for osx10.11 (x86_64)
--
-- Host: localhost    Database: webglobeserver
-- ------------------------------------------------------
-- Server version	5.7.16

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
-- Table structure for table `netcdf_dataset_fields`
--
CREATE DATABASE IF NOT EXISTS webglobeserver;
DROP TABLE IF EXISTS `netcdf_dataset_fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `netcdf_dataset_fields` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `dataset_id` mediumint(9) DEFAULT NULL,
  `field_name` char(30) NOT NULL,
  `units` char(30) DEFAULT NULL,
  `max_value` float DEFAULT NULL,
  `min_value` float DEFAULT NULL,
  `hdfs_path` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `netcdf_dataset_fields_foreign_key_1` (`dataset_id`),
  CONSTRAINT `netcdf_dataset_fields_foreign_key_1` FOREIGN KEY (`dataset_id`) REFERENCES `netcdf_datasets` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `netcdf_datasets`
--

DROP TABLE IF EXISTS `netcdf_datasets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `netcdf_datasets` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `name` char(64) DEFAULT NULL,
  `user` varchar(64) DEFAULT NULL,
  `info` text,
  `info_url` char(100) DEFAULT NULL,
  `is_accessible` tinyint(4) DEFAULT NULL,
  `lon_min` double DEFAULT NULL,
  `lon_max` double DEFAULT NULL,
  `lon_num` int(11) DEFAULT NULL,
  `lat_min` double DEFAULT NULL,
  `lat_max` double DEFAULT NULL,
  `lat_num` int(11) DEFAULT NULL,
  `time_min` datetime DEFAULT NULL,
  `time_max` datetime DEFAULT NULL,
  `time_num` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submitted_analysis_jobs`
--

DROP TABLE IF EXISTS `submitted_analysis_jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `submitted_analysis_jobs` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `user_name` varchar(20) DEFAULT NULL,
  `dataset_id` mediumint(9) DEFAULT NULL,
  `analysis` varchar(60) NOT NULL,
  `field` varchar(60) DEFAULT NULL,
  `status` char(10) DEFAULT NULL,
  `submission_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `result_loc` char(120) DEFAULT NULL,
  `priority` char(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `submitted_jobs_foreign_key_1` (`user_name`),
  KEY `submitted_jobs_foreign_key_2` (`dataset_id`),
  CONSTRAINT `submitted_jobs_foreign_key_1` FOREIGN KEY (`user_name`) REFERENCES `tomcat_users` (`user_name`),
  CONSTRAINT `submitted_jobs_foreign_key_2` FOREIGN KEY (`dataset_id`) REFERENCES `netcdf_datasets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submitted_upload_jobs`
--

DROP TABLE IF EXISTS `submitted_upload_jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `submitted_upload_jobs` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `user_name` varchar(20) DEFAULT NULL,
  `dataset_url` varchar(120) DEFAULT NULL,
  `dataset_name` varchar(60) DEFAULT NULL,
  `status` char(10) DEFAULT NULL,
  `submission_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `priority` char(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `submitted_image_creation_jobs_foreign_key_1` (`user_name`),
  CONSTRAINT `submitted_image_creation_jobs_foreign_key_1` FOREIGN KEY (`user_name`) REFERENCES `tomcat_users` (`user_name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
