-- 이전 버전에서 거절 확정 후 PENDING으로 남은 정기 청구를 종료한다.
-- 결제 시도가 없거나 미확정·승인·보상 이력이 섞인 청구는 변경하지 않는다.
update billings b
set status = 'CANCELLED'
where b.purpose = 'RECURRING'
  and b.status = 'PENDING'
  and exists (select 1 from payments p where p.billing_id = b.id and p.status = 'FAILED')
  and not exists (select 1 from payments p where p.billing_id = b.id and p.status <> 'FAILED');
