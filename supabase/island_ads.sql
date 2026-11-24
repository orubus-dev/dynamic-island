-- Orubus Island · собственная реклама продуктов Orubus
-- Выполнить один раз в SQL Editor ЛЮБОГО существующего проекта Supabase
-- (новый проект не создаётся). Приложение читает таблицу анонимным ключом.

create table if not exists public.island_ads (
    id        text primary key,
    title     text not null,
    text      text not null default '',
    url       text not null,
    image     text,
    weight    int  not null default 1,
    active    boolean not null default true,
    created_at timestamptz not null default now()
);

alter table public.island_ads enable row level security;

drop policy if exists "island ads are public" on public.island_ads;
create policy "island ads are public"
    on public.island_ads for select
    to anon
    using (true);

-- Пример строки:
-- insert into public.island_ads (id, title, text, url, weight) values
--     ('relay', 'Relay', 'Realtime messaging — Android + Windows, E2EE.', 'https://github.com/orubus-dev/Relay', 3)
-- on conflict (id) do nothing;

-- Приложение берёт URL/ключ проекта из секретов сборки ISLAND_SUPABASE_URL / ISLAND_SUPABASE_ANON_KEY.
-- Пока они не вписаны, остров показывает store/ads.json из репозитория.
