package me.hoon.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import me.hoon.querydsl.dto.MemberSearchCondition;
import me.hoon.querydsl.dto.MemberTeamDto;
import me.hoon.querydsl.dto.QMemberTeamDto;
import me.hoon.querydsl.entitiy.Member;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static me.hoon.querydsl.entitiy.QMember.member;
import static me.hoon.querydsl.entitiy.QTeam.team;

@RequiredArgsConstructor
@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }


    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member).fetch();
    }
    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = where(condition);

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername())
                      .and(teamNameEq(condition.getTeamName()))
                      .and(ageGoe(condition.getAgeGoe()))
                      .and(ageLoe(condition.getAgeLoe()))
                      )
                .fetch();
    }

    private BooleanBuilder usernameEq(String username) {
        if(!StringUtils.hasText(username)) {
            return new BooleanBuilder();
        }

        return new BooleanBuilder(member.username.eq(username));
    }

    private BooleanBuilder teamNameEq(String teamName) {
        if(!StringUtils.hasText(teamName)) {
            return new BooleanBuilder();
        }

        return new BooleanBuilder(team.name.eq(teamName));
    }

    private BooleanBuilder ageGoe(Integer ageGoe) {
        if(ageGoe == null) {
            return new BooleanBuilder();
        }

        return new BooleanBuilder(member.age.goe(ageGoe));
    }

    private BooleanBuilder ageLoe(Integer ageLoe) {
        if(ageLoe == null) {
            return new BooleanBuilder();
        }

        return new BooleanBuilder(member.age.loe(ageLoe));
    }


    private BooleanBuilder where(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if(StringUtils.hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }

        if (StringUtils.hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return builder;
    }
}
