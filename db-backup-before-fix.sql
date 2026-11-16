-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: toefl_db
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `suite_section`
--

LOCK TABLES `suite_section` WRITE;
/*!40000 ALTER TABLE `suite_section` DISABLE KEYS */;
INSERT INTO `suite_section` VALUES (1,1,1,'reading','2026-11-10 19:33:09'),(2,1,2,'listening','2026-11-10 19:33:09'),(3,1,3,'speaking','2026-11-10 19:33:09'),(4,1,4,'writing','2026-11-10 19:33:09'),(5,2,1,'reading',NULL),(6,2,2,'listening',NULL),(7,2,3,'speaking',NULL),(8,2,4,'writing',NULL),(9,3,1,'reading',NULL),(10,3,2,'listening',NULL),(11,3,3,'speaking',NULL),(12,3,4,'writing',NULL),(13,4,1,'reading',NULL),(14,4,2,'listening',NULL),(15,4,3,'speaking',NULL),(16,4,4,'writing',NULL),(17,5,1,'reading',NULL),(18,5,2,'listening',NULL),(19,5,3,'speaking',NULL),(20,5,4,'writing',NULL);
/*!40000 ALTER TABLE `suite_section` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `tpo_section`
--

LOCK TABLES `tpo_section` WRITE;
/*!40000 ALTER TABLE `tpo_section` DISABLE KEYS */;
INSERT INTO `tpo_section` VALUES (1,1,'reading','The origins of theater are a matter of debate among scholars. Some theories link theater to ritual ceremonies. Early humans held ceremonies to ask for favorable weather or hunt success. Gradually, these rituals became less religious and more entertaining, and theater appeared.',NULL,NULL),(4,1,'speaking','Speaking Tasks: Independent Speaking & Integrated Speaking',NULL,'2026-11-13 16:27:22'),(5,1,'writing','Writing Tasks: Independent Writing & Integrated Writing',NULL,'2026-11-13 16:27:22'),(6,1,'reading','The development of agriculture transformed human society. Before farming, people relied on hunting and gathering. They moved frequently to find food. Once plants were domesticated, humans could settle down in villages. Settled life led to population growth, specialized jobs and the invention of writing. Scholars argue that agriculture was the foundation of civilization.',NULL,'2026-11-13 16:17:47'),(7,1,'listening','Conversation between a student and librarian. The student wants to borrow a reference book for his history paper. The book cannot be checked out, but he can read it in the library and copy pages. The librarian tells him the opening time of the reading room.','/audio/tpo01-conversation1.mp3','2026-11-13 16:17:47');
/*!40000 ALTER TABLE `tpo_section` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `tpo_question`
--

LOCK TABLES `tpo_question` WRITE;
/*!40000 ALTER TABLE `tpo_question` DISABLE KEYS */;
INSERT INTO `tpo_question` VALUES (60,1,NULL,'VOCABULARY: The word “ceremonies” in the passage is closest in meaning to','ritual activities','daily meals','trade events','farming tools','A','Analysis: A ceremony is a formal ritual activity. Other options do not match the meaning of the word in this context.',NULL,NULL,'reading',NULL,NULL,NULL,NULL,NULL,0,NULL),(61,1,NULL,'FACTUAL: According to the passage, why did early humans hold ceremonies?','To record historical stories','To ask for good weather and hunting success','To perform theater shows','To teach children skills','B','Analysis: The passage states that early humans held ceremonies to pray for favorable weather or successful hunting. B matches the original text.',NULL,NULL,'reading',NULL,NULL,NULL,NULL,NULL,0,NULL),(62,1,NULL,'INFERENCE: What can be inferred about rituals over time?','They stayed exactly the same','They became less religious and more entertaining','They disappeared immediately after theater appeared','They were only performed by actors','B','Analysis: The sentence \"Gradually, these rituals became less religious and more entertaining\" supports this inference.',NULL,NULL,'reading',NULL,NULL,NULL,NULL,NULL,0,NULL),(63,1,NULL,'RHETORICAL PURPOSE: Why does the author mention “early humans held ceremonies”?','To argue that theater is useless','To explain the origin of theater','To compare different kinds of entertainment','To prove religion has no influence on culture','B','Analysis: The whole paragraph focuses on the theory that theater developed from ancient rituals. The author uses this detail to explain the origin of theater.',NULL,NULL,'reading',NULL,NULL,NULL,NULL,NULL,0,NULL),(64,1,NULL,'NEGATIVE FACTUAL: All of the following are mentioned in the passage EXCEPT','Rituals were related to religion','Rituals gradually changed','Theater developed from rituals','Theater was more important than farming','D','Analysis: The passage never compares theater and farming or says theater is more important. D is NOT mentioned.',NULL,NULL,'reading',NULL,NULL,NULL,NULL,NULL,0,NULL),(65,1,NULL,'SENTENCE SIMPLIFICATION: Which sentence best simplifies the sentence: Gradually, these rituals became less religious and more entertaining, and theater appeared.','Rituals changed slowly, lost some religious meaning, turned into entertainment, and theater came out.','Theater was religious at the very beginning.','People stopped rituals completely and started theater directly.','Entertainment was more important than religion for ancient people.','A','Analysis: \"Gradually\" = slowly; \"less religious and more entertaining\" matches the meaning in option A. It keeps all core information of the original sentence.',NULL,NULL,'reading',NULL,NULL,NULL,NULL,NULL,0,NULL);
/*!40000 ALTER TABLE `tpo_question` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-13 18:25:15
