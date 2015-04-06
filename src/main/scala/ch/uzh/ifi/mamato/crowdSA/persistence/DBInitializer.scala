package ch.uzh.ifi.mamato.crowdSA.persistence

/**
 * Created by Mattia on 19.01.2015.
 */

import ch.uzh.ifi.mamato.crowdSA.util.LazyLogger
import scalikejdbc._

object DBInitializer extends LazyLogger {

  def run() {
    DB readOnly { implicit s =>
      //Papers TABLE
      try {
        sql"select 1 from papers limit 1".map(_.long(1)).single.apply()
        logger.debug("Papers already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE papers (id BIGINT NOT NULL AUTO_INCREMENT,title VARCHAR(255) NOT NULL, budget_cts INT NOT NULL, remote_id BIGINT NOT NULL, PRIMARY KEY(id));".execute().apply()
            sql"INSERT INTO papers(title, budget_cts, remote_id) values ('Test paper', 1000, 1);".execute.apply()
          }
          logger.debug("Table Papers created!")
      }

      //discovery TABLE
      try {
        sql"select 1 from discovery limit 1".map(_.long(1)).single.apply()
        logger.debug("Discovery already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE discovery (id BIGINT NOT NULL AUTO_INCREMENT,description VARCHAR(10000) NULL, start_time varchar(100) NULL, end_time varchar(100) NULL, result VARCHAR(8000) NULL, error VARCHAR(2000) NULL, cost INT NULL, PRIMARY KEY(id));".execute().apply()
            //sql"INSERT INTO discovery(description, budget_cts, remote_id) values ('Test paper', 1000, 1);".execute.apply()
          }
          logger.debug("Table Discovery created!")
      }

      //Questions TABLE
      try {
        sql"select 1 from questions limit 1".map(_.long(1)).single.apply()
        logger.debug("Questions already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE questions (id BIGINT NOT NULL AUTO_INCREMENT,question VARCHAR(255) NOT NULL,question_type VARCHAR(255) NOT NULL,reward_cts INT NOT NULL,created_at BIGINT NOT NULL,remote_paper_id BIGINT NOT NULL,remote_question_id BIGINT NOT NULL, disabled BIT NOT NULL, maximal_assignments INT NULL, expiration_time_sec BIGINT NULL, possible_answers VARCHAR(2000) NULL, PRIMARY KEY(id));".execute().apply()
            sql"INSERT INTO questions(question, question_type, reward_cts, created_at, remote_paper_id, remote_question_id, disabled, maximal_assignments, expiration_time_sec, possible_answers) values ('Test question','Boolean',10,123123123,1,1, false, NULL, NULL, NULL);".execute.apply()
          }
          logger.debug("Table Questions created!")
      }

      //Stat_methods TABLE
      try {
        sql"select 1 from stat_methods limit 1".map(_.long(1)).single.apply()
        logger.debug("Stat_methods already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE stat_methods (id BIGINT NOT NULL AUTO_INCREMENT, stat_method VARCHAR(255) NOT NULL, PRIMARY KEY(id));".execute().apply()

            sql"INSERT INTO stat_methods(stat_method) values ('MANOVA');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('ANOVA');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('ANCOVA');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('linear regression');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('logistic regression');".execute.apply()

            sql"INSERT INTO stat_methods(stat_method) values ('correlation');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('mann-whitney test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('wilcoxon signed-ranks test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('krusakal-wallis test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('mcnamar');".execute.apply()

            sql"INSERT INTO stat_methods(stat_method) values ('friedman');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for population variance');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for an assumed population variance');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square test for compatibility of K counts');".execute.apply()

            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a 2x2 table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a kx2 table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a 2xk table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a pxq table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for a suitable probabilistic model');".execute.apply()

            sql"INSERT INTO stat_methods(stat_method) values ('t-test for two population means');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('t-test for a population mean');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('t-test for two population means');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('t-test for two population means');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('t-test of a regression coefficient');".execute.apply()

            sql"INSERT INTO stat_methods(stat_method) values ('t-test of a correlation coefficient');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('factor analysis');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('cluster analysis');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('survival analysis');".execute.apply()
          }
          logger.debug("Table Stat_methods created!")
      }

      //Assumptions TABLE
      try {
        sql"select 1 from assumptions limit 1".map(_.long(1)).single.apply()
        logger.debug("Stat_methods already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE assumptions (id BIGINT NOT NULL AUTO_INCREMENT, assumption VARCHAR(255) NOT NULL, url VARCHAR(255) NULL, PRIMARY KEY(id));".execute().apply()

            sql"INSERT INTO assumptions(assumption, url) values ('Normality', 'http://en.wikipedia.org/wiki/Normality_test');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Linearity', 'http://www.utexas.edu/courses/schwab/sw388r7/SolvingProblems/AssumptionOfLinearity.ppt');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Homogeneity of variances', 'http://en.wikipedia.org/wiki/Homogeneity_%28statistics%29');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Constant Variance', 'https://www.statisticssolutions.com/homoscedasticity/');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Independence', 'http://en.wikipedia.org/wiki/Independence_%28probability_theory%29');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Homogeneity of regression slopes', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Independence of Error terms', 'http://www.pages.drexel.edu/~tpm23/STAT902/DWTest.pdf');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Dependent variable must be a dichotomy', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Independent variables need not be interval, nor normally distributed, nor linearly related, nor of equal variance within each group', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Larger samples are needed than for linear regression', null);".execute.apply()

            sql"INSERT INTO assumptions(assumption, url) values ('Variables must be either interval or ratio measurements', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Outliers are either kept to a minimum or are removed entierly', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Responses are ordinal', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Random samples from populations', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Data is measured at least on an ordinal scale, no normality needed', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Each pair is chosen randomly and independently', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Continuous distrubutions are the same for the test variables', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Case represent random samples from the populations', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Pair are matched', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Existence of one group that is measured on three or more different occasions', null);".execute.apply()

            sql"INSERT INTO assumptions(assumption, url) values ('Group is random sample from population', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('One dependant variable that is either ordinal interval or ratio', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Samples does not need to be normally distributed', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Quantitative data', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Independent observations', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Adequate sample size (at least 10)', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Simple random sample', 'http://en.wikipedia.org/wiki/Simple_random_sample');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Data in frequency form', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('No particular distribution assumed', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Sample Size', null);".execute.apply()

            sql"INSERT INTO assumptions(assumption, url) values ('Cell frequencies', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('K classes', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Interval classification', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Error has constant variance and is on average equals 0', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('No association between the factor and measurement error', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('No association between errors', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Sample represent the population', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Variables are not correlated', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Non-informative censoring', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Proportional hazards', null);".execute.apply()
          }
          logger.debug("Table Stat_methods created!")
      }

      //Stat_method2Assumptions TABLE
      try {
        sql"select 1 from stat_method2assumptions limit 1".map(_.long(1)).single.apply()
        logger.debug("Stat_methods already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE stat_method2assumptions (id BIGINT NOT NULL AUTO_INCREMENT, stat_method_id BIGINT NOT NULL, assumption_id BIGINT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(assumption_id) REFERENCES assumptions(id), FOREIGN KEY(stat_method_id) REFERENCES stat_methods(id));".execute().apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (1,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (1,2);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (1,3);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (1,4);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (2,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (2,3);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (2,4);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (2,5);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (3,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (3,2);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (3,3);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (3,6);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (3,7);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (4,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (4,4);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (4,5);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (4,7);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (5,8);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (5,9);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (5,10);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (6,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (6,4);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (6,11);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (6,12);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (7,5);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (7,13);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (7,14);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (8,5);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (8,15);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (8,16);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (9,5);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (9,17);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (9,18);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (10,19);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (11,20);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (11,21);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (11,22);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (11,23);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (12,24);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (12,25);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (12,26);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (12,27);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (12,28);".execute.apply()

            //chi square for population variance
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (13,1);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (14,1);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (15,29);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (16,30);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (16,31);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (17,31);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (18,30);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (18,32);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (19,31);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (20,33);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (21,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (21,5);".execute.apply()
            // chi square for consistency in a 2xk table
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (22,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (22,5);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (23,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (23,5);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (24,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (24,5);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (25,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (25,4);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (26,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (26,2);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (27,34);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (27,35);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (27,36);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (27,5);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (28,37);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (28,38);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (29,39);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (29,40);".execute.apply()

          }
          logger.debug("Table Stat_methods created!")
      }

      //Assumptions2Questions TABLE
      try {
        sql"select 1 from assumption2questions limit 1".map(_.long(1)).single.apply()
        logger.debug("Stat_methods already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE assumption2questions (id BIGINT NOT NULL AUTO_INCREMENT, assumption_id BIGINT NOT NULL, question VARCHAR(1000) NOT NULL, test_names VARCHAR(255) NOT NULL, PRIMARY KEY(id), FOREIGN KEY(assumption_id) REFERENCES assumptions(id));".execute().apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the De Agostino K-squared test used to test the normality assumption?', 'Agostino,K-squared,K2,K test,kurtosis,skewness');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Jarque-Bera test used to test the normality?', 'Jarque–Bera,JB test');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Anderson-Darling test used to test the normality?', 'Anderson–Darling,K-sample');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Cramér-von Mises criterion used to test the normality?', 'Cramér–von Mises criterion');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Lilliefors test used to test the normality?', 'Lilliefors');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Kolmogorov-Smirnov test used to test the normality?', 'Kolmogorov–Smirnov,K–S test');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Shapiro-Wilk test used to test the normality?', 'Shapiro-Wilk');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Pearsons chi-squared test used to test the normality?', 'Pearson,chi-squared,χ2');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Shapiro-Francia test used to test the normality?', 'Shapiro-Francia');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Did they plot a histogram/graph and it looked normal distributed?', 'Histogram,plot,normal distribution');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Is the correlation test used to test the linearity?', 'correlation coefficient,significance,Pearson');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Did they mentioned that the relationships are linear?', 'linearity');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Are r^2 tests executed?', 'coefficient of determination,R^2,r^2,R2,R squared');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Did they use the anova test of linearity?', 'anova,linearity,linear regression');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Did they used a graphical method to test the linearity?', 'plot,diagram,histogram,diagram,chart,box plot,graph,heatmap,pie chart,plotting,scatterplot,skewplot,sparkline,stemplot,radar chart');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Does the eta correlation ratio shows linearity?', 'eta,correlation ratio,statistical dispersion,standard deviations');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Do the plot variables reveals linearity?', 'plot,diagram,histogram,diagram,chart,box plot,graph,heatmap,pie chart,plotting,scatterplot,skewplot,sparkline,stemplot,radar chart,linearity');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (3, 'Is the Box M test used to test the Homogeneity of variance?', 'Box M,Homogeneity,Covariance,Matrices');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Did they use some graphical methods to test the Homoscedasticity (AKA Constant Variance)?', 'plot,histogram,diagram,chart,box plot,graph,heatmap,pie chart,plotting,scatterplot,skewplot,sparkline,stemplot,radar chart');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the WLS regression used to test the Homoscedasticity (AKA Constant Variance)?', 'least square,weighted least square,weighted linear least square,Ω,correlation matrix,residuals');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the Goldfeld Quandt test used to test the Homoscedasticity (AKA Constant Variance)?', 'Goldfeld–Quandt,homoscedasticity');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the Glejser test used to test the Homoscedasticity (AKA Constant Variance)?', 'Glejser,heteroscedasticity,regression,residuals,ordinary least square,sample residuals,R2');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the Park test used to test the Homoscedasticity (AKA Constant Variance)?', 'Park,linear regression heteroscedasticity');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the Breusch Pagan Godfrey test used to test the Homoscedasticity (AKA Constant Variance)?', 'Breusch–Pagan,Godfrey,linear regression,heteroscedasticity,variance,residuals,regression');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the White test used to test the Homoscedasticity (AKA Constant Variance)?', 'White,residual variance,regression,homoscedasticity,heteroscedasticity');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the Levene test used to test the Homoscedasticity (AKA Constant Variance)?', 'Levene,variance,homogenity of variance,homoscedasticity');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (5, 'Is the Fisher exact test used to test the Independence?', 'Fisher,exact test,Fisher-Yates,Fisher-Irwin,chi^2,chi-squared');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (5, 'Is the G-test used to test the Independence?', 'G-test,likelihood-ratio,maximum likelihood,statistical significance,chi-squared');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (5, 'Is the Hilbert-Schmidt independence criterion used to test the Independence?', 'Hilbert-Schmidt,independence criterion');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (5, 'Is the Schweizer-Wolff approach used to test the Independence?', 'Schweizer-wolff');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (6, 'Are there some interaction between the covariance and the independent variable?', 'covariance,independent variable');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (7, 'Is the Durbin-Watson used to test the Independence of error terms?', 'Durbin-watson,autocorrelation,residuals,regression analysis');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (8, 'Is the dependent variable a dichotomy?', 'dichotomy');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (9, 'The type of the independent variables need not be interval, nor normally distributed, nor linearly related, nor of equal variance within each group. Is this true?', 'distributed,linear,variance,independent variable');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (10, 'Are the samples larger than for the linear regression?', 'sample,population,linearity,regression');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (11, 'Are the variables: intervals or ratio measurements?', 'intervals,ratio');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (12, 'Are the outliers kept to a minimum and/or are removed entierly?', 'outliers,minimum,removed');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (13, 'Are the responses ordinal?', 'ordinal');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (14, 'Are the samples of the populations random?', 'population,random,samples');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (15, 'Are the datas measured at least on an ordinal scale?', 'ordinal');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (16, 'Are the pairs chosen randomly and independently?', 'random,independently');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (17, 'Are the Continuous distributions the same for all the test variables?', 'continuous,distribution,variables');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (18, 'Is the case a random samples from the populations?', 'random,samples,population');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (19, 'Are the pair matched?', 'pair');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (20, 'There exists at least one group that is measured on three or more different occasions?', 'measured,occasions');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (21, 'Is the group analyzed a random sample from the populations?', 'random,sample,population');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (22, 'Is one dependant variable either an ordinal interval or ratio?', 'dependant,variable,ordinal,interval,ratio');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (23, 'Are the samples normally distributed?', 'normality,normal,distribution,samples');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (24, 'Are the data quantitative?', 'data,quantitative');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (25, 'Are the observation independent?', 'independent,observation');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (26, 'Is the sample size at least 10?', 'sample, population,size');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (27, 'Is the sample of the type simple random?', 'simple,random,sample');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (28, 'Is the data in frequency form?', 'frequency');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (29, 'Are particular distribution assumed?', 'distribution,assumption,assumed');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (30, 'Is the sample size greather than 20?', '20,twenty,sample,size,population');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (31, 'Are the cell frequencies greather than 3?', '3,three,frequency,frequencies,cell,cells');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (32, 'Are the K classes, when put together, a complete series?', 'K classes,K class,complete series');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (33, 'Have both distributions the same interval classification and the same number of elements?', 'cell,cell frequency');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (34, 'Has the error a constant variance that is on average equals 0?', 'average,zero,0,error,constant variance');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (35, 'There is an association between the factor and the measurement error?', 'factor,association,measurement,error');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (36, 'There is an association between the errors?', 'error,errors,association');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (37, 'Do the samples represent the population?', 'sample,samples,population,populations');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (38, 'Are the variables correlated?', 'correlation,correlated');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (39, 'Is the dataset non-informative censored?', 'non-informative,censored');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (40, 'Are the hazards proportional?', 'hazards,hazard,proportional');".execute.apply()
          }
          logger.debug("Table Stat_methods created!")
      }
    }
  }
}