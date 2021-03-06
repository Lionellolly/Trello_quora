package com.upgrad.quora.service.business;

import com.upgrad.quora.service.dao.QuestionDao;
import com.upgrad.quora.service.dao.UserDao;
import com.upgrad.quora.service.entity.QuestionEntity;
import com.upgrad.quora.service.entity.UserAuthEntity;
import com.upgrad.quora.service.entity.UserEntity;
import com.upgrad.quora.service.exception.AuthorizationFailedException;
import com.upgrad.quora.service.exception.InvalidQuestionException;
import com.upgrad.quora.service.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.List;

@Service
public class QuestionService {
    @Autowired
    private QuestionDao questionDao;

    @Autowired
    private UserDao userDao;

    //This service class holds the logic for the functioning of the Question controller and connection with the Question Dao layer.
    
    @Transactional(propagation = Propagation.REQUIRED)
    public QuestionEntity createQuestion(final String accessToken, QuestionEntity questionEntity) throws AuthorizationFailedException {
        UserAuthEntity authEntity = userDao.getUserAuthByToken(accessToken);
        if (authEntity == null){
            final String code = "ATHR-001";
            final String comment = "User has not signed in";
            throw new AuthorizationFailedException(code, comment);
        }
        if (authEntity.getLogoutAt() != null){
            final String code = "ATHR-002";
            final String comment = "User is signed out.Sign in first to post a question";
            throw new AuthorizationFailedException(code, comment);
        }
        questionEntity.setDate(ZonedDateTime.now());
        questionEntity.setUuid(UUID.randomUUID().toString());
        questionEntity.setUserEntity(authEntity.getUserEntity());
        return questionDao.createQuestion(questionEntity);
    }

    public List<QuestionEntity> getAllQuestions(final String accessToken)
            throws AuthorizationFailedException {
        UserAuthEntity userAuthEntity = userDao.getUserAuthByToken(accessToken);
        if (userAuthEntity == null) {
            throw new AuthorizationFailedException("ATHR-001", "User has not signed in");
        } else if (userAuthEntity.getLogoutAt() != null) {
            throw new AuthorizationFailedException(
                    "ATHR-002", "User is signed out.Sign in first to get all questions");
        }
        return questionDao.getAllQuestions();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public QuestionEntity editQuestion(
            final String accessToken, final String questionId, final String content)
            throws AuthorizationFailedException, InvalidQuestionException {
        UserAuthEntity userAuthEntity = userDao.getUserAuthByToken(accessToken);
        if (userAuthEntity == null) {
            final String code = "ATHR-001";
            final String comment = "User has not signed in";
            throw new AuthorizationFailedException(code, comment);
        } else if (userAuthEntity.getLogoutAt() != null) {
            final String code = "ATHR-002";
            final String comment = "User is signed out.Sign in first to edit the question";
            throw new AuthorizationFailedException( code, comment);
        }
        QuestionEntity questionEntity = questionDao.getQuestionById(questionId);
        if (questionEntity == null) {
            final String code = "QUES-001";
            final String comment = "Entered question uuid does not exist";
            throw new InvalidQuestionException(code , comment);
        }
        boolean flag = !questionEntity
                .getUserEntity()
                .getUuid()
                .equals(userAuthEntity.getUserEntity().getUuid());
        if (flag) {
            final String code = "ATHR-003";
            final String comment = "Only the question owner can edit the question";
            throw new AuthorizationFailedException(code ,comment);
        }
        questionEntity.setContent(content);
        questionDao.updateQuestion(questionEntity);
        return questionEntity;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public QuestionEntity deleteQuestion(final String accessToken, final String questionId)
            throws AuthorizationFailedException, InvalidQuestionException {
        UserAuthEntity userAuthEntity = userDao.getUserAuthByToken(accessToken);
        if (userAuthEntity == null) {
            final String code = "ATHR-001";
            final String comment = "User has not signed in";
            throw new AuthorizationFailedException(code, comment);
        } else if (userAuthEntity.getLogoutAt() != null) {
            final String code = "ATHR-002";
            final String comment = "User is signed out.Sign in first to delete the question";
            throw new AuthorizationFailedException(code, comment);
        }
        QuestionEntity questionEntity = questionDao.getQuestionById(questionId);
        if (questionEntity == null) {
            final String code = "QUES-001";
            final String comment = "Entered question uuid does not exist";
            throw new InvalidQuestionException(code,comment);
        }
        boolean cond1 = !questionEntity.getUserEntity().getUuid().equals(userAuthEntity.getUserEntity().getUuid());
        boolean cond2 = !userAuthEntity.getUserEntity().getRole().equals("admin");
        if (cond1 && cond2) {
            final String code = "ATHR-003";
            final String comment = "Only the question owner or admin can delete the question";
            throw new AuthorizationFailedException(code, comment);
        }

        questionDao.deleteQuestion(questionEntity);
        return questionEntity;
    }

    public List<QuestionEntity> getAllQuestionsByUser(final String userId, final String accessToken)
            throws AuthorizationFailedException, UserNotFoundException {
        UserAuthEntity userAuthEntity = userDao.getUserAuthByToken(accessToken);
        if (userAuthEntity == null) {
            final String code = "ATHR-001";
            final String comment = "User has not signed in";
            throw new AuthorizationFailedException(code, comment);
        } else if (userAuthEntity.getLogoutAt() != null) {
            final String code = "ATHR-002";
            final String comment = "User is signed out.Sign in first to get all questions posted by a specific user";
            throw new AuthorizationFailedException(code, comment);
        }
        UserEntity user = userDao.getUserById(userId);
        if (user == null) {
            final String code = "USR-001";
            final String comment = "User with entered uuid whose question details are to be seen does not exist";
            throw new UserNotFoundException(code, comment);
        }
        return questionDao.getAllQuestionsByUser(user);
    }

}
