DROP TABLE IF EXISTS software;
DROP TABLE IF EXISTS CPE_ENTRY;
DROP TABLE IF EXISTS reference;
DROP TABLE IF EXISTS vulnerability;

CREATE TABLE vulnerability (id int auto_increment PRIMARY KEY, version int, lastModified timestamp, cve VARCHAR(20) UNIQUE,
	description VARCHAR(8000), cwe VARCHAR(10), cvssScore DECIMAL(3,1), cvssAccessVector VARCHAR(20),
	cvssAccessComplexity VARCHAR(20), cvssAuthentication VARCHAR(20), cvssConfidentialityImpact VARCHAR(20),
	cvssIntegrityImpact VARCHAR(20), cvssAvailabilityImpact VARCHAR(20));

CREATE TABLE reference (id int auto_increment PRIMARY KEY, version int, lastModified timestamp, cveid INT, name VARCHAR(1000), url VARCHAR(1000), source VARCHAR(255));
ALTER TABLE reference ADD FOREIGN KEY (cveid) REFERENCES vulnerability (id);
CREATE UNIQUE INDEX reference_unique ON reference(cveid, name, url, source);

CREATE TABLE CPE_ENTRY (id INT auto_increment PRIMARY KEY, version int, lastModified timestamp, cpe VARCHAR(250), vendor VARCHAR(255), product VARCHAR(255));

CREATE TABLE software (id int auto_increment PRIMARY KEY, version int, lastModified timestamp, cveid INT, cpeEntryId INT, previousVersion VARCHAR(50));
CREATE UNIQUE INDEX software_unique ON software(cveid, cpeEntryId);
ALTER TABLE software ADD FOREIGN KEY (cveid) REFERENCES vulnerability (id);
ALTER TABLE software ADD FOREIGN KEY (cpeEntryId) REFERENCES CPE_ENTRY (id);