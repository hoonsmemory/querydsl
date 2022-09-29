package me.hoon.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.hoon.querydsl.dto.MemberDto;
import me.hoon.querydsl.dto.QMemberDto;
import me.hoon.querydsl.entitiy.Member;
import me.hoon.querydsl.entitiy.QMember;
import me.hoon.querydsl.entitiy.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static me.hoon.querydsl.entitiy.QMember.member;

@Transactional
@SpringBootTest
public class QuerydslAdvanceTest {

    @Autowired
    EntityManager em;

    @Autowired
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //em.flush();
        //em.clear();
    }

    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        String username = result.get(0).get(member.username);
        Integer age = result.get(0).get(member.age);

        System.out.println(username + " : " + age);
    }

    //프로퍼티 접근 방법
    @Test
    public void findDtoBySetter() {
        queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void findDtoByField() {
        queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void findDtoByConstructor() {
        queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void findDtoByFieldAndSubQueryAlias() {

        QMember memberSub = new QMember("memberSub");

        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username.as("username"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")))
                .from(member)
                .fetch();


        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    //BooleanBuilder 사용해서 username = member1, age = 10 찾기
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String username = "member1";
        Integer age = 10;

        List<Member> members = searchMember1(null, null);

    }

    private List<Member> searchMember1(String username, Integer age) {

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if(username != null) {
            booleanBuilder.and(member.username.eq(username));
        }

        if(age != null) {
            booleanBuilder.and(member.age.eq(age));
        }

        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .where(booleanBuilder)
                .fetch();

        return result;
    }

    //WhereParam 사용해서 username = member1, age = 10 찾기
    @Test
    public void dynamicQuery_WhereParam() {
        String username = "member1";
        Integer age = 10;

        List<Member> members = searchMember2(null, null);

    }

    private List<Member> searchMember2(String username, Integer age) {

        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .where(usernameEq(username).and(ageEq(age)))
                .fetch();

        return result;
    }

    private BooleanBuilder usernameEq(String username) {
        if(username == null) {
            return new BooleanBuilder();
        }

        return new BooleanBuilder(member.username.eq(username));
    }

    private BooleanBuilder ageEq(Integer age) {
        if(age == null) {
            return new BooleanBuilder();
        }

        return new BooleanBuilder(member.age.eq(age));
    }

    //벌크 연산
    @Commit
    @Test
    public void bulkUpdate() {

        //DB : Member(id=1, username=member1, age=10)
        //DB : Member(id=2, username=member2, age=20)
        //DB : Member(id=3, username=member3, age=30)
        //DB : Member(id=4, username=member4, age=40)
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.loe(20))
                .execute();

        em.flush();
        em.clear();
        //DB : Member(id=1, username=비회원, age=10)
        //DB : Member(id=2, username=비회원, age=20)
        //DB : Member(id=3, username=member3, age=30)
        //DB : Member(id=4, username=member4, age=40)
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    public void bulkAddAge() {
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    //DB 방언에 따라 제공되는 기능도 다르다.
    @Test
    public void sqlFunction() {
        String result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetchFirst();

        System.out.println(result);

        List<String> lower = queryFactory
                .select(member.username)
                .from(member)
                //.where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();


        for (String s : lower) {
            System.out.println(s);
        }

    }

}
